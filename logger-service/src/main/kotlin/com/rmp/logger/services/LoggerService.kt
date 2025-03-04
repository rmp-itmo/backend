package com.rmp.logger.services

import com.rmp.lib.utils.korm.insert
import com.rmp.lib.utils.korm.query.QueryDto
import com.rmp.lib.utils.korm.query.prepare
import com.rmp.lib.utils.log.Logger
import com.rmp.lib.utils.redis.RedisEvent
import com.rmp.lib.utils.redis.fsm.FsmService
import com.rmp.logger.conf.ServiceConf
import com.rmp.logger.models.LogModel
import com.zaxxer.hikari.HikariDataSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.channels.actor
import org.kodein.di.DI

class LoggerService(di: DI): FsmService(di) {
    private val clickhouseDatasource = HikariDataSource().apply {
        jdbcUrl = ServiceConf.dbConf.jdbcUrl
        driverClassName = ServiceConf.dbConf.driverClassName
        username = ServiceConf.dbConf.username
        password = ServiceConf.dbConf.password
    }

    private class ClickhouseEvent(
        val queryDto: QueryDto
    )

    @OptIn(ObsoleteCoroutinesApi::class)
    private val clickHouseActor = CoroutineScope(Job()).actor<ClickhouseEvent> {
        for (event in this) {
            val connection = clickhouseDatasource.connection ?: continue

            val stmt = event.queryDto.prepare(connection)
            stmt.executeQuery()

            connection.close()
        }
    }

    suspend fun processEvent(redisEvent: RedisEvent) {
        val logEvent =
            redisEvent.parseData<Logger.LogEvent.TraceLogEvent>(silent = true) ?:
            redisEvent.parseData<Logger.LogEvent.ExceptionLogEvent>(silent = true) ?:
            redisEvent.parseData<Logger.LogEvent.SimpleLogEvent>(silent = true) ?:
            redisEvent.parseData<Logger.LogEvent>() ?: return

        val query: QueryDto = LogModel.insert {

            it[ts] = logEvent.ts
            it[sender] = logEvent.sender
            it[prefix] = logEvent.prefix
            it[message] = logEvent.data
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

        clickHouseActor.send(ClickhouseEvent(query))
    }
}