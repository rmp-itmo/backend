@file:OptIn(ExperimentalUuidApi::class)

package com.rmp.lib.utils.redis

import com.rmp.lib.shared.conf.AppConf
import com.rmp.lib.utils.kodein.KodeinService
import com.rmp.lib.utils.korm.query.batch.BatchBuilder
import com.rmp.lib.utils.log.Logger
import com.rmp.lib.utils.serialization.Json
import io.lettuce.core.RedisClient
import io.lettuce.core.pubsub.api.sync.RedisPubSubCommands
import io.micrometer.core.instrument.Timer
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.actor
import org.kodein.di.DI
import java.util.concurrent.TimeUnit
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

open class PubSubService(val serviceName: String, prometheusMeterRegistry: PrometheusMeterRegistry, di: DI) : KodeinService(di) {
    private val client: RedisPubSubCommands<String, String> by lazy {
        RedisClient.create("redis://${AppConf.redis.host}:${AppConf.redis.port}").connectPubSub().sync()
    }

    private val dbProcessingTime: Timer = Timer
        .builder(AppConf.DB_REQUEST_PROCESSING_TIME_METRIC)
        .publishPercentiles(0.5, 0.95, 0.99)
        .register(prometheusMeterRegistry)

    private val eventPublishingTime : Timer = Timer
        .builder("event-publishing-time")
        .publishPercentiles(0.5, 0.95, 0.99)
        .register(prometheusMeterRegistry)

    val json = Json.serializer

    fun publish(event: RedisEvent, channel: String, from: String? = null) {
        val time = System.currentTimeMillis()
        event.from = from ?: serviceName
        Logger.traceEventPublished(event, channel)
        client.publish(channel, json.encodeToString(event))
        eventPublishingTime.record(System.currentTimeMillis() - time, TimeUnit.MILLISECONDS)
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

    @OptIn(ObsoleteCoroutinesApi::class, ExperimentalUuidApi::class)
    private val dbActor = CoroutineScope(Job()).actor<DbCommunication>(capacity = Channel.UNLIMITED) {
        for (item in this) {
            when (item) {
                is DbCommunication.DbRequest -> {
                    var id = Uuid.random()
                    while (id in pendingDbRequests) { id = Uuid.random() }
                    item.idFetched.complete(id)
                    pendingDbRequests += id to item.completableDeferred
                    time += id to System.currentTimeMillis()
                    Logger.debug("Pending request added $id", action = item.redisEvent.action)
                }
                is DbCommunication.DbResponse -> {
                    if (pendingDbRequests.containsKey(item.value.dbRequest)) {
                        Logger.debug("Pending request found ${item.value.dbRequest}", action = item.value.action)
                        val deferred = pendingDbRequests.getValue(item.value.dbRequest!!)

                        val timeout = System.currentTimeMillis() - time[item.value.dbRequest]!!

                        dbProcessingTime.record(timeout, TimeUnit.MILLISECONDS)

                        if (deferred.isCompleted) {
                            publish(item.value.mutateData(BatchBuilder.build { rollback() }), AppConf.redis.db)
                            pendingDbRequests -= item.value.dbRequest!!
                            continue
                        }

                        val result = try {
                            item.value.parseDb()
                        } catch (e: Exception) {
                            Logger.debugException("Failed to parse db response. Response: \n ${item.value}", e, "database", item.value.action)
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
