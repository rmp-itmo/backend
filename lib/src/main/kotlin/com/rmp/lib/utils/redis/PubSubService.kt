package com.rmp.lib.utils.redis

import com.rmp.lib.shared.conf.AppConf
import com.rmp.lib.utils.kodein.KodeinService
import com.rmp.lib.utils.log.Logger
import com.rmp.lib.utils.serialization.Json
import io.github.crackthecodeabhi.kreds.connection.Endpoint
import io.github.crackthecodeabhi.kreds.connection.newClient
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.channels.actor
import org.kodein.di.DI

open class PubSubService(val serviceName: String, di: DI) : KodeinService(di) {
    val client by lazy {
        newClient(Endpoint(AppConf.redis.host, AppConf.redis.port))
    }

    val json = Json.serializer

    suspend inline fun publish(event: RedisEvent, channel: String, from: String? = null) {
        event.from = from ?: serviceName
        client.use {
            Logger.traceEventPublished(event, channel)
            it.publish(channel, json.encodeToString(event))
        }
    }

    private val pendingDbRequests = mutableMapOf<Long, Pair<RedisEvent, CompletableDeferred<DbResponseData>>>()

    private sealed class DbCommunication(val id: Long) {
        class DbRequest(id: Long, val redisEvent: RedisEvent, val completableDeferred: CompletableDeferred<DbResponseData>): DbCommunication(id)
        class DbResponse(id: Long, val value: RedisEvent): DbCommunication(id)
    }

    @OptIn(ObsoleteCoroutinesApi::class)
    private val dbActor = CoroutineScope(Job()).actor<DbCommunication> {
        for (item in this) {
            when (item) {
                is DbCommunication.DbRequest -> {
                    pendingDbRequests += item.id to (item.redisEvent to item.completableDeferred)
                    Logger.debug("Pending request added $item")
                    Logger.debug("$pendingDbRequests")
                }
                is DbCommunication.DbResponse -> {
                    if (pendingDbRequests.containsKey(item.id)) {
                        Logger.debug("Pending request found ${pendingDbRequests[item.id]}")
                        val (initiator, deferred) = pendingDbRequests.getValue(item.id)
                        initiator.tid = item.value.tid
                        Logger.debug("DB RESPONSE RECEIVED: ${item.value}")

                        val result = try {
                            item.value.parseDb()
                        } catch (e: Exception) {
                            Logger.debugException("Failed to parse db response", e, "db")
                            null
                        }

                        if (result == null) deferred.complete(DbResponseData(mutableMapOf()))
                        else deferred.complete(result)

                        pendingDbRequests -= item.id
                    }
                }
            }
        }
    }

    fun regDbRequest(id: Long, redisEvent: RedisEvent): CompletableDeferred<DbResponseData> {
        val completable = CompletableDeferred<DbResponseData>()
        dbActor.trySend(DbCommunication.DbRequest(id, redisEvent, completable))
        return completable
    }

    fun processDbResponse(id: Long, redisEvent: RedisEvent) {
        dbActor.trySend(DbCommunication.DbResponse(id, redisEvent))
    }
}
