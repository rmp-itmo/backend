package com.rmp.api.utils.api

import com.rmp.api.prometheusRegistry
import com.rmp.lib.shared.conf.AppConf
import com.rmp.lib.shared.modules.auth.dto.AuthorizedUser
import com.rmp.lib.utils.redis.*
import io.micrometer.core.instrument.Timer
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.actor
import org.kodein.di.DI
import java.util.concurrent.TimeUnit
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class ApiService(di: DI): PubSubService(AppConf.redis.api, prometheusRegistry, di) {
    private val awaited: MutableMap<String, CompletableDeferred<String>> = mutableMapOf()
    private val time: MutableMap<String, Long> = mutableMapOf()
    private val executionTime = Timer
        .builder("request-total-processing-time")
//        .publishPercentileHistogram(true)
        .publishPercentiles(0.5, 0.95, 0.99)
        .register(prometheusRegistry)


    sealed class ApiEvent(val redisEvent: RedisEvent) {
        class IncomingEvent(
            redisEvent: RedisEvent
        ): ApiEvent(redisEvent)

        class OutcomeEvent(
            redisEvent: RedisEvent,
            val onComplete: CompletableDeferred<String>,
            val to: String
        ): ApiEvent(redisEvent)
    }

    @OptIn(ObsoleteCoroutinesApi::class, ExperimentalUuidApi::class)
    private val apiActor = CoroutineScope(Job()).actor<ApiEvent>(capacity = Channel.BUFFERED) {
        for (item in this) {
            when (item) {
                is ApiEvent.IncomingEvent -> {
                    val action = item.redisEvent.action

                    if (awaited.containsKey(action)) {
                        val deferred = awaited[action]!!
                        executionTime.record(System.currentTimeMillis() - time[action]!!, TimeUnit.MILLISECONDS)
                        deferred.complete(item.redisEvent.data)
                        awaited.remove(action)
                    }

                }

                is ApiEvent.OutcomeEvent -> {
                    var action = "${Uuid.random()}-${item.redisEvent.eventType}"
                    while (action in awaited) action = "${Uuid.random()}-${item.redisEvent.eventType}"
                    item.redisEvent.action = action

                    time += item.redisEvent.action to System.currentTimeMillis()
                    awaited += item.redisEvent.action to item.onComplete

                    publish(item.redisEvent, item.to)
                }
            }
        }
    }


    @OptIn(ExperimentalUuidApi::class)
    suspend fun process(eventType: String, data: String, to: String, authorizedUser: AuthorizedUser? = null): CompletableDeferred<String> {
        val redisEvent = RedisEvent("action", AppConf.redis.api, eventType, RedisEventState(RedisEventState.State.INIT), data).authorize(authorizedUser)

        val result = CompletableDeferred<String>()

        apiActor.send(ApiEvent.OutcomeEvent(redisEvent, result, to))

        return result
    }

    fun receive(redisEvent: RedisEvent) {
        apiActor.trySend(ApiEvent.IncomingEvent(redisEvent))
    }
}