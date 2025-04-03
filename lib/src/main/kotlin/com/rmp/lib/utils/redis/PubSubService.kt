@file:OptIn(ExperimentalUuidApi::class)

package com.rmp.lib.utils.redis

import com.rmp.lib.shared.conf.AppConf
import com.rmp.lib.utils.kodein.KodeinService
import com.rmp.lib.utils.korm.query.BatchQuery
import com.rmp.lib.utils.korm.query.QueryDto
import com.rmp.lib.utils.korm.query.QueryType
import com.rmp.lib.utils.korm.query.batch.BatchBuilder
import com.rmp.lib.utils.log.Logger
import com.rmp.lib.utils.serialization.Json
import io.github.crackthecodeabhi.kreds.connection.Endpoint
import io.github.crackthecodeabhi.kreds.connection.newClient
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.actor
import org.kodein.di.DI
import java.util.UUID
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

const val DB_REQUEST_TIMEOUT = 5_000L

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

    private val time = mutableMapOf<Uuid, Long>()
    private val pendingDbRequests = mutableMapOf<Uuid, CompletableDeferred<DbResponseData>>()

    private sealed class DbCommunication() {
        class DbRequest(
            val redisEvent: RedisEvent,
            val completableDeferred: CompletableDeferred<DbResponseData>,
            val idFetched: CompletableDeferred<Uuid>
        ): DbCommunication()
        class DbResponse(val value: RedisEvent): DbCommunication()
    }

    @OptIn(ObsoleteCoroutinesApi::class)
    private val dbActor = CoroutineScope(Job()).actor<DbCommunication>(capacity = Channel.UNLIMITED) {
        for (item in this) {
            when (item) {
                is DbCommunication.DbRequest -> {
                    var id = Uuid.random()
                    while (id in pendingDbRequests) { id = Uuid.random() }
                    item.idFetched.complete(id)
                    pendingDbRequests += id to item.completableDeferred
                    time += id to System.currentTimeMillis()
                    Logger.debug("Pending request added ${item.redisEvent.action}")
                }
                is DbCommunication.DbResponse -> {
                    if (pendingDbRequests.containsKey(item.value.dbRequest)) {
                        Logger.debug("Pending request found ${item.value.action}")
                        val deferred = pendingDbRequests.getValue(item.value.dbRequest!!)

                        val timeout = System.currentTimeMillis() - time[item.value.dbRequest]!!

                        Logger.debug("DB RESPONSE RECEIVED[${timeout}ms]: ${item.value}")

                        if (deferred.isCompleted) {
                            publish(item.value.mutateData(BatchBuilder.build { rollback() }), AppConf.redis.db)
                            pendingDbRequests -= item.value.dbRequest!!
                            continue
                        }

                        val result = try {
                            item.value.parseDb()
                        } catch (e: Exception) {
                            Logger.debugException("Failed to parse db response", e, "db")
                            null
                        }

                        if (result == null) deferred.complete(DbResponseData(mutableMapOf()))
                        else deferred.complete(result)

                        pendingDbRequests -= item.value.dbRequest!!
                    }
                }
            }
        }
    }

    suspend fun regDbRequest(redisEvent: RedisEvent): Pair<CompletableDeferred<Uuid>, CompletableDeferred<DbResponseData>> {
        val completable = CompletableDeferred<DbResponseData>()
        val idFetched = CompletableDeferred<Uuid>()
        dbActor.send(DbCommunication.DbRequest(redisEvent, completable, idFetched))
        return idFetched to completable
    }

    fun processDbResponse(redisEvent: RedisEvent) {
        dbActor.trySend(DbCommunication.DbResponse(redisEvent))
    }
}
