package com.rmp.lib.utils.log

import com.rmp.lib.shared.conf.AppConf
import com.rmp.lib.utils.redis.RedisEvent
import com.rmp.lib.utils.redis.RedisEventState
import com.rmp.lib.utils.redis.SerializableClass
import com.rmp.lib.utils.serialization.Json
import io.lettuce.core.RedisClient
import io.lettuce.core.pubsub.api.sync.RedisPubSubCommands
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.actor
import kotlinx.serialization.Serializable
import org.slf4j.LoggerFactory
import kotlin.uuid.ExperimentalUuidApi


@OptIn(ExperimentalUuidApi::class)
object Logger {
    private val pubClient: RedisPubSubCommands<String, String> by lazy {
        RedisClient.create("redis://${AppConf.redis.host}:${AppConf.redis.port}").connectPubSub().sync()
    }

    val serviceName = AppConf.logger.serviceName

    @Serializable
    sealed class LogEvent(
        val eventType: LogEventType
    ): SerializableClass {
        abstract val ts: Long
        abstract val sender: String
        abstract val prefix: String
        abstract val data: String
        abstract val severity: String

        @Serializable
        data class TraceLogEvent(
            override val ts: Long,
            override val sender: String,
            override val prefix: String,
            override val data: String,
            val actionId: String,
            val dest: String,
            override val severity: String = "INFO",
        ): LogEvent(LogEventType.EVENT_TRACE)

        @Serializable
        data class SimpleLogEvent(
            override val ts: Long,
            override val sender: String,
            override val prefix: String,
            override val data: String,
            override val severity: String,
            val actionId: String,
        ): LogEvent(LogEventType.SIMPLE)

        @Serializable
        data class ExceptionLogEvent(
            override val ts: Long,
            override val sender: String,
            override val prefix: String,
            override val data: String,
            val cause: String,
            val stacktrace: String,
            val actionId: String,
            override val severity: String = "DEBUG",
        ): LogEvent(LogEventType.EXCEPTION)
    }

    @OptIn(ObsoleteCoroutinesApi::class)
    private val loggerActor = CoroutineScope(Job()).actor<LogEvent>(capacity = Channel.UNLIMITED) {
        for (event in this) {
            val re = RedisEvent(
                "log-${System.nanoTime()}",
                serviceName,
                "log",
                RedisEventState("", ""),
                Json.serializer.encodeToString(event)
            )
            pubClient.publish(AppConf.redis.logger, Json.serializer.encodeToString(re))
        }
    }

    private val logger = mutableMapOf(
        "main" to LoggerFactory.getLogger("com.rmp.main"),
        "database" to LoggerFactory.getLogger("Database"),
        "transformation" to LoggerFactory.getLogger("Transformation"),
        "trace" to LoggerFactory.getLogger("Trace"),
    )

    fun debug(message: Any?, prefix: String = "main", action: String = "") {
        val messageSerializable = message != null && message::class.annotations.any { it.annotationClass == Serializable::class }

        val simpleLogEvent = LogEvent.SimpleLogEvent(
            System.currentTimeMillis(),
            serviceName,
            prefix,
            if (messageSerializable) Json.serializer.encodeToString(message)
            else message.toString(),
            "DEBUG",
            action
        )
        if (action != "") loggerActor.trySend(simpleLogEvent)
        logger[prefix].let {
            val logger = if (it == null) {
                logger["main"]!!.debug("Logger $prefix not found")
                logger["main"]!!
            } else it
            logger.debug(message.toString())
        }
    }

    fun debugException(message: Any?, cause: Throwable, prefix: String, action: String) {
        val simpleLogEvent = LogEvent.ExceptionLogEvent(
            System.currentTimeMillis(),
            serviceName,
            prefix,
            message.toString(),
            cause.message ?: cause.toString(),
            cause.stackTraceToString(),
            action
        )
        loggerActor.trySend(simpleLogEvent)

        logger[prefix].let {
            val logger = if (it == null) {
                logger["main"]!!.debug("Logger $prefix not found")
                logger["main"]!!
            } else it

            logger.debug(message.toString())
            logger.debug("Something failed due to $cause")
            logger.debug("Stacktrace => ${cause.stackTraceToString()}")
        }
    }

    fun traceEventPublished(redisEvent: RedisEvent, dest: String) {
        val info = "Event published by $serviceName to $dest \n" +
                    "${redisEvent.eventType}.${redisEvent.eventState.state} <${redisEvent.action}> {\n" +
                        "\tData: ${redisEvent.data} \n" +
                        "\tState: ${redisEvent.eventState.stateData} \n" +
                        "\tDb Request: ${redisEvent.dbRequest}" +
                    "}"

        val traceLogEvent = LogEvent.TraceLogEvent(
            ts = System.currentTimeMillis(),
            sender = serviceName,
            prefix = "tracer",
            actionId = redisEvent.action,
            data = info,
            dest = dest
        )

        loggerActor.trySend(traceLogEvent)

        logger["trace"]!!.info(info)
    }

    fun traceEventReceived(redisEvent: RedisEvent) {
//        if (redisEvent.from == AppConf.redis.db)
            logger["trace"]!!.info(
                "Event received from ${redisEvent.from} \n" +
                        "${redisEvent.eventType}.${redisEvent.eventState.state} <${redisEvent.action}> {\n" +
                        "\tData: ${
                            if (redisEvent.data.length > 201)
                                redisEvent.data.slice(0..200)
                            else
                                redisEvent.data
                        } \n" +
                        "\tState: ${
//                            if (redisEvent.eventState.stateData != null && redisEvent.eventState.stateData.length > 201)
//                                redisEvent.eventState.stateData.slice(0..200)
//                            else
                                redisEvent.eventState.stateData
                        } \n" +
                        "}"
            )
//        else
//            logger["trace"]!!.info(
//                "Event received from ${redisEvent.from} \n" +
//                        "${redisEvent.eventType}.${redisEvent.eventState.state} <${redisEvent.action}> {\n" +
//                        "\tData: ${redisEvent.data} \n" +
//                        "\tState: ${redisEvent.eventState.stateData} \n" +
//                        "}"
//            )
    }
}
