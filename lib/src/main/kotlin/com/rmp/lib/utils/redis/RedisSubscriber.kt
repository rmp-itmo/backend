package com.rmp.lib.utils.redis

import com.rmp.lib.shared.conf.AppConf
import com.rmp.lib.utils.log.Logger
import com.rmp.lib.utils.serialization.Json
import io.github.crackthecodeabhi.kreds.connection.AbstractKredsSubscriber
import io.github.crackthecodeabhi.kreds.connection.Endpoint
import io.github.crackthecodeabhi.kreds.connection.KredsSubscriber
import io.github.crackthecodeabhi.kreds.connection.newSubscriberClient
import kotlinx.coroutines.*
import kotlin.time.Duration

abstract class RedisSubscriber: AbstractKredsSubscriber() {
    override fun onPSubscribe(pattern: String, subscribedChannels: Long) {
        Logger.debug("PSubscribed to channel: $pattern")
    }

    override fun onPMessage(pattern: String, channel: String, message: String) {}

    override fun onSubscribe(channel: String, subscribedChannels: Long) {
        Logger.debug("Subscribed to channel: $channel")
    }

    override fun onUnsubscribe(channel: String, subscribedChannels: Long) {
        Logger.debug("Unsubscribed from channel: $channel")
    }

    override fun onException(ex: Throwable) {
        TODO("Not yet implemented")
    }

    override fun onMessage(channel: String, message: String) {
        val redisEvent = try {
            val event = Json.serializer.decodeFromString<RedisEvent>(message)
            Logger.traceEventReceived(event)
            event
        } catch (e: IllegalArgumentException) {
            Logger.debugException("Failed to parse redis event", e, "trace")
            null
        }

        onMessage(redisEvent, channel, message)
    }

    abstract fun onMessage(redisEvent: RedisEvent?, channel: String, message: String)
}

suspend fun CoroutineScope.subscribe(handler: KredsSubscriber, blocking: Boolean = true, vararg channels: String) = withContext(coroutineContext) {
    newSubscriberClient(Endpoint(AppConf.redis.host, AppConf.redis.port), handler).use { client ->
        channels.forEach { client.subscribe(it) }
        if (blocking) {
            delay(Duration.INFINITE)
        }
    }
}