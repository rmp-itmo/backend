package com.rmp.api.utils.api

import com.rmp.lib.shared.conf.AppConf
import com.rmp.lib.shared.modules.auth.dto.AuthorizedUser
import com.rmp.lib.utils.redis.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.actor
import org.kodein.di.DI
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

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

    @OptIn(ObsoleteCoroutinesApi::class, ExperimentalUuidApi::class)
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
                    var action = "${Uuid.random()}-${item.redisEvent.eventType}"
                    while (action in awaited) action = "${Uuid.random()}-${item.redisEvent.eventType}"
                    item.redisEvent.action = action

                    awaited += Pair(item.redisEvent.action, item.onComplete)

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