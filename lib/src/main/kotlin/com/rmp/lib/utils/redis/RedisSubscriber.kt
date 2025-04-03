package com.rmp.lib.utils.redis

import com.rmp.lib.shared.conf.AppConf
import com.rmp.lib.utils.log.Logger
import com.rmp.lib.utils.serialization.Json
import io.lettuce.core.RedisClient
import io.lettuce.core.pubsub.RedisPubSubListener
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection
import io.lettuce.core.pubsub.api.sync.RedisPubSubCommands
import kotlinx.coroutines.*
import kotlin.time.Duration


suspend fun subscribe(redisSubscriber: RedisSubscriber, blocking: Boolean = true, channel: String) {
    val client: RedisClient = RedisClient.create("redis://${AppConf.redis.host}:${AppConf.redis.port}")

    val connection: StatefulRedisPubSubConnection<String, String> = client.connectPubSub()
    connection.addListener(redisSubscriber)

    val sync: RedisPubSubCommands<String, String>  = connection.sync()
    sync.subscribe(channel)

    if (blocking) delay(Duration.INFINITE)
}


abstract class RedisSubscriber: RedisPubSubListener<String, String> {
    override fun message(channel: String?, message: String?) {
        if (channel == null || message == null) return

        val redisEvent = try {
            val event = Json.serializer.decodeFromString<RedisEvent>(message)
            Logger.traceEventReceived(event)
            event
        } catch (e: IllegalArgumentException) {
            Logger.debugException("Failed to parse redis event", e, "trace", "")
            null
        }

        onMessage(redisEvent, channel, message)
    }

    override fun message(var1: String?, var2: String?, var3: String?) {}

    override fun subscribed(channel: String?, subscribers: Long) {
        Logger.debug("Subscribed to channel: $channel")
    }

    override fun psubscribed(var1: String?, var2: Long) {}

    override fun unsubscribed(var1: String?, var2: Long) {}

    override fun punsubscribed(var1: String?, var2: Long) {}

    abstract fun onMessage(redisEvent: RedisEvent?, channel: String, message: String)
}