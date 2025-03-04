package com.rmp.logger

import com.rmp.lib.utils.korm.TableRegister
import com.rmp.lib.utils.korm.initTable
import com.rmp.lib.utils.korm.query.QueryDto
import com.rmp.lib.utils.korm.query.prepare
import com.rmp.logger.conf.ServiceConf
import com.zaxxer.hikari.HikariDataSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.actor

object ConnectionManager {
    private val clickhouseDatasource = HikariDataSource().apply {
        jdbcUrl = ServiceConf.dbConf.jdbcUrl
        driverClassName = ServiceConf.dbConf.driverClassName
        username = ServiceConf.dbConf.username
        password = ServiceConf.dbConf.password
    }

    fun initTables() {
        TableRegister.tables.forEach { (_, entry) ->
            val (type, table) = entry
            clickHouseActor.trySend(ClickhouseEvent(table.initTable(dbType = type, forceRecreate = false)))
        }
    }

    private class ClickhouseEvent(
        val queryDto: QueryDto
    )

    @OptIn(ObsoleteCoroutinesApi::class)
    private val clickHouseActor = CoroutineScope(Job()).actor<ClickhouseEvent>(capacity = Channel.BUFFERED) {
        for (event in this) {
            val connection = clickhouseDatasource.connection ?: continue

            val stmt = event.queryDto.prepare(connection)
            stmt.executeQuery()

            connection.close()
        }
    }

    fun execute(queryDto: QueryDto) {
        clickHouseActor.trySend(ClickhouseEvent(queryDto))
    }
}