package com.rmp.logger.services

import com.rmp.lib.utils.korm.insert
import com.rmp.lib.utils.korm.query.QueryDto
import com.rmp.lib.utils.log.Logger
import com.rmp.lib.utils.redis.RedisEvent
import com.rmp.lib.utils.redis.fsm.FsmService
import com.rmp.logger.ConnectionManager
import com.rmp.logger.models.LogModel
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.kodein.di.DI
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.*

class LoggerService(di: DI): FsmService(di) {
    fun processEvent(redisEvent: RedisEvent) {
        val logEvent =
            redisEvent.parseData<Logger.LogEvent.TraceLogEvent>(silent = true) ?:
            redisEvent.parseData<Logger.LogEvent.ExceptionLogEvent>(silent = true) ?:
            redisEvent.parseData<Logger.LogEvent.SimpleLogEvent>(silent = true) ?:
            redisEvent.parseData<Logger.LogEvent>() ?: return

        val query: QueryDto = LogModel.insert {
            val localDateTime = LocalDateTime.ofInstant(
                Instant.ofEpochMilli(logEvent.ts),
                ZoneOffset.UTC
            )
            it[ts] = localDateTime.toString()
            it[labels] = buildJsonObject {
                put("sender", logEvent.sender)
                put("prefix", logEvent.prefix)
            }.toString()
            it[message] = "${logEvent.prefix.uppercase(Locale.getDefault())} [${logEvent.sender}] ${logEvent.data}"
            it[severity] = logEvent.severity

            when (logEvent) {
                is Logger.LogEvent.TraceLogEvent -> {
                    it[type] = "TraceLog"
                    it[actionId] = logEvent.actionId
                    it[destination] = logEvent.dest
                }

                is Logger.LogEvent.ExceptionLogEvent -> {
                    it[type] = "ExceptionLog"
                    it[cause] = logEvent.cause
                    it[stackTrace] = logEvent.stacktrace
                }

                is Logger.LogEvent.SimpleLogEvent -> {
                    it[type] = "SimpleLog"
                }
            }
        }

        ConnectionManager.execute(query)
    }
}