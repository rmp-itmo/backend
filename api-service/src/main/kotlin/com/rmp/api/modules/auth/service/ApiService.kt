package com.rmp.api.modules.auth.service

import com.rmp.lib.shared.conf.AppConf
import com.rmp.lib.utils.redis.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.actor
import org.kodein.di.DI

class ApiService(di: DI): PubSubService(AppConf.redis.api, di) {
    private val awaited: MutableMap<String, CompletableDeferred<String>> = mutableMapOf()

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

    @OptIn(ObsoleteCoroutinesApi::class)
    private val apiActor = CoroutineScope(Job()).actor<ApiEvent>(capacity = Channel.BUFFERED) {
        for (item in this) {
            when (item) {
                is ApiEvent.IncomingEvent -> {
                    val action = item.redisEvent.action

                    if (awaited.containsKey(action)) {
                        val deferred = awaited[action]!!
                        deferred.complete(item.redisEvent.data)
                    }
                }

                is ApiEvent.OutcomeEvent -> {
                    awaited += Pair(item.redisEvent.action, item.onComplete)

                    publish(item.redisEvent, item.to)
                }
            }
        }
    }

    suspend fun process(eventType: String, data: String, to: String): CompletableDeferred<String> {
        val action = "${System.currentTimeMillis()}-$eventType"
        val redisEvent = RedisEvent(action, AppConf.redis.api, eventType, RedisEventState("INIT"), data)

        val result = CompletableDeferred<String>()

        apiActor.send(ApiEvent.OutcomeEvent(redisEvent, result, to))

        return result
    }

    fun receive(redisEvent: RedisEvent) {
        apiActor.trySend(ApiEvent.IncomingEvent(redisEvent))
    }
}