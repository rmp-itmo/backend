package com.rmp.lib.utils.redis

import com.rmp.lib.shared.conf.AppConf
import com.rmp.lib.utils.kodein.KodeinService
import com.rmp.lib.utils.log.Logger
import com.rmp.lib.utils.serialization.Json
import io.github.crackthecodeabhi.kreds.connection.Endpoint
import io.github.crackthecodeabhi.kreds.connection.newClient
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
}