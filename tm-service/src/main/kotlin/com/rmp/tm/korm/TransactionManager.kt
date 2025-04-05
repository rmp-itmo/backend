package com.rmp.tm.korm

import com.rmp.lib.utils.korm.RowDto
import com.rmp.lib.utils.korm.Table
import com.rmp.lib.utils.korm.TableRegister
import com.rmp.lib.utils.korm.initTable
import com.rmp.lib.utils.korm.query.*
import com.rmp.lib.utils.korm.query.batch.BatchBuilder
import com.rmp.lib.utils.log.Logger
import com.rmp.lib.utils.redis.RedisEvent
import com.rmp.lib.utils.redis.RedisEventState
import com.rmp.tm.prometheusMeterRegistry
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.micrometer.core.instrument.Timer
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.actor
import org.postgresql.util.PSQLException
import java.sql.Connection
import java.util.concurrent.TimeUnit
import kotlin.uuid.ExperimentalUuidApi

data class ProcessingState(
    val redisEvent: RedisEvent
) {
    var connection: Connection? = null
    var executionResult: Map<String, ExecutionResult> = emptyMap()
}

object TransactionManager {
    private lateinit var ds: HikariDataSource

    private var maximumPoolSize = 90
    private val active: MutableMap<String, Connection> = mutableMapOf()

    private val pending: ArrayDeque<ConnectionEvent> = ArrayDeque()

    fun init(config: HikariConfig.() -> Unit) {
        ds = HikariDataSource(HikariConfig().apply(config).apply {
            isAutoCommit = false
            maximumPoolSize = this@TransactionManager.maximumPoolSize
            connectionTimeout = 250
        })
    }

    @OptIn(ExperimentalUuidApi::class)
    suspend fun initTables(forceRecreate: Boolean = false, excludedFromRecreation: Set<Table> = mutableSetOf(), initScript: (BatchBuilder.() -> Unit)? = null) {
        val connection = ds.connection
        val dummyEvent = RedisEvent("initialization-event", "", "", RedisEventState(""), "")
        val state = ProcessingState(dummyEvent)
        state.connection = connection
        active += dummyEvent.action to connection

        TableRegister.tables.forEach { (_, entry) ->
            val (type, table) = entry
            val query = table.initTable(dbType = type, forceRecreate = forceRecreate && table !in excludedFromRecreation)
//            Logger.debug("Init table ${table.tableName_} - ${query.sql} $connection", "database")
            try {
                executeNoResult(state, query, query.queryParseData ?: mutableMapOf())
            } catch (e: PSQLException) {
                val relationNotFound = Regex("ERROR: relation \"([^.]*)\" does not exist")
                val (tableName) = relationNotFound.find(e.message ?: "")?.destructured ?: throw e
                if (e.message?.matches(relationNotFound) == true) {
                    try {
                        connection.rollback()
                        connection.close()
                    } catch (_: Exception) {}
                    throw Exception("Failed to init table '${table.tableName_}' relation '$tableName' must be created first")
                } else throw e
            }
        }
        connection.commit()
        connection.close()
        active -= dummyEvent.action

        val query = BatchBuilder.buildAutoCommit(initScript ?: return)
        process(dummyEvent, query)
    }

    val processedChannel: Channel<ProcessingState> = Channel(capacity = Channel.UNLIMITED)

    sealed class ConnectionEvent(val state: ProcessingState, val finished: CompletableDeferred<Unit>) {
//        class GetConnection(state: ProcessingState, finished: CompletableDeferred<Unit>): ConnectionEvent(state, finished)
        class InitConnection(state: ProcessingState, finished: CompletableDeferred<Unit>): ConnectionEvent(state, finished)
        class Commit(state: ProcessingState, finished: CompletableDeferred<Unit>): ConnectionEvent(state, finished)
        class Rollback(state: ProcessingState, finished: CompletableDeferred<Unit>): ConnectionEvent(state, finished)
    }

    private fun initConnection(action: String): Connection? {
        return try {
            val pair = action to ds.connection
            active += pair
            pair.second
        } catch (e: Exception) {
            null
        }
    }

    private fun commit(action: String, connection: Connection) {
        if (!connection.isClosed) {
            connection.commit()
            connection.close()
            Logger.debug("COMMIT SUCCEED! CONNECTION $action", "database")
        } else {
            Logger.debug("COMMIT FAILED! CONNECTION $action CLOSED", "database")
        }
    }

    private fun rollback(action: String, connection: Connection) {
        if (!connection.isClosed) {
            connection.rollback()
            connection.close()
            Logger.debug("ROLLBACK SUCCEED!", "database", action)
        } else {
            Logger.debug("ROLLBACK FAILED! CONNECTION CLOSED", "database", action)
        }
    }

    //!!!!!!!! call it ONLY from ACTOR scope !!!!!!!!
    private suspend fun sendPendingIfExists() {
        val isPending = pending.removeFirstOrNull()
        if (isPending != null) processConnectionEvent(isPending)
    }

    private suspend fun processConnectionEvent(ev: ConnectionEvent) {
        val action = ev.state.redisEvent.action

        when (ev) {
            is ConnectionEvent.InitConnection -> {
                if (active.size >= maximumPoolSize) {
                    pending += ev
                    return
                }

                val connection = initConnection(action)

                if (connection == null) {
                    pending += ev
                    return
                }

                active += action to connection
                ev.state.connection = connection

                ev.finished.complete(Unit)
            }
            is ConnectionEvent.Commit -> {
                val connection = ev.state.connection.let {
                    if (it == null) Logger.debug("COMMIT FAILED! NO OPEN CONNECTION", "database", action)
                    it
                } ?: run {
                    ev.finished.complete(Unit)
                    return
                }

                commit(action, connection)
                active.remove(action)
                sendPendingIfExists()

                ev.state.connection = null
                ev.finished.complete(Unit)
            }
            is ConnectionEvent.Rollback -> {
                val connection = ev.state.connection.let {
                    if (it == null) Logger.debug("ROLLBACK FAILED! NO OPEN CONNECTION", "database", action)
                    it
                } ?: run {
                    ev.finished.complete(Unit)
                    return
                }

                rollback(action, connection)
                active.remove(action)
                sendPendingIfExists()

                ev.state.connection = null
                ev.finished.complete(Unit)
            }
        }
    }

    @OptIn(ObsoleteCoroutinesApi::class)
    val connectionActor = CoroutineScope(Job()).actor<ConnectionEvent>(capacity = Channel.UNLIMITED) {
        for (ev in this) {
            processConnectionEvent(ev)
        }
    }

    private fun execute(state: ProcessingState, query: QueryDto, columns: List<String>, insert: Boolean = false): List<RowDto> {
        val start = System.currentTimeMillis()
        val connection = state.connection ?: throw IllegalStateException("Connection $state is null")

        val stmt = query.prepare(connection)

        val rs = try {
            if (!insert) stmt.executeQuery()
            else {
                stmt.executeUpdate()
                stmt.generatedKeys
            }
        } catch (e: Exception) {
            Logger.debugException("Failed to process query ${query.sql}, params: ${query.params}", e, "database", state.redisEvent.action)
            rollback(state.redisEvent.action, connection)
            throw e
        }

        val result = mutableListOf<RowDto>()
        while (rs.next()) {
            result += RowDto.build(rs, columns)
        }

        Logger.debug("(${query.sql}) execution time: ${System.currentTimeMillis() - start}", "database", state.redisEvent.action)

        return result
    }

    private fun executeNoResult(state: ProcessingState, query: QueryDto, queryParseData: QueryParseData): List<RowDto> {
        val start = System.currentTimeMillis()
        val connection = state.connection ?: throw IllegalStateException("Connection $state is null")

        val stmt = query.prepare(connection)

        val result = try {
            stmt.executeUpdate()
        } catch (e: Exception) {
            Logger.debugException("Failed to process query", e, "database", state.redisEvent.action)
            rollback(state.redisEvent.action, connection)
            throw e
        }

        Logger.debug("(${query.sql}) execution time: ${System.currentTimeMillis() - start}", "database", state.redisEvent.action)

        val tableName = queryParseData.keys.firstOrNull() ?: ""

        return tableName.let {
            if (it == "") listOf(RowDto(mutableMapOf("result" to result)))
            else listOf(RowDto(mutableMapOf("\"$it\".\"result\"" to result)))
        }
    }

    val processingTimer = Timer
        .builder("db-internal-process-time")
        .publishPercentiles(0.5, 0.95, 0.99)
        .register(prometheusMeterRegistry)

    suspend fun process(redisEvent: RedisEvent, batch: BatchQuery) {
        val time = System.currentTimeMillis()
        val state = ProcessingState(redisEvent)

        if (redisEvent.action in active) {
            state.connection = active[redisEvent.action]!!
            Logger.debug("Connection restored. ACTION: ${redisEvent.action}", "database", action = redisEvent.action)
        }

        var failed = false
        state.executionResult = batch.queries.mapNotNull { (label, query) ->
            if (failed) return@mapNotNull null
            Logger.debug("[${QueryType.getByValue(query.queryType)}] ${query.sql} ${query.params}, Conn=${state.connection}", "database", redisEvent.action)

            val executionResult = when (QueryType.getByValue(query.queryType)) {
                QueryType.INIT -> {
                    val connectionInit = CompletableDeferred<Unit>()
                    connectionActor.send(ConnectionEvent.InitConnection(state, connectionInit))
                    connectionInit.await()
                    null
                }
                QueryType.SELECT, QueryType.INSERT -> {
                    val res = try {
                        execute(
                            state,
                            query,
                            query.queryParseData?.map { (tableName, columns) ->
                                columns.map { "\"$tableName\".\"$it\"" }
                            }?.flatten() ?: listOf(),
                            query.queryType == QueryType.INSERT.value
                        )
                    } catch (e: Exception) {
                        failed = true
                        emptyList()
                    }

                    label to ExecutionResult(query.queryParseData ?: mutableMapOf(), res)
                }
                QueryType.UPDATE, QueryType.DELETE -> {
                    val queryParseData = query.queryParseData ?: mutableMapOf()
                    val res = try {
                        executeNoResult(
                            state, query, queryParseData
                        )
                    } catch (e: Exception) {
                        failed = true
                        emptyList()
                    }

                    label to ExecutionResult(queryParseData, res)
                }

                QueryType.COMMIT -> {
                    val commit = CompletableDeferred<Unit>()
                    connectionActor.send(ConnectionEvent.Commit(state, commit))
                    commit.await()
                    null
                }
                QueryType.ROLLBACK -> {
                    val rollback = CompletableDeferred<Unit>()
                    connectionActor.send(ConnectionEvent.Rollback(state, rollback))
                    rollback.await()
                    null
                }

                else -> {
                    Logger.debug("Unknown query type ${query.queryType}", "database", redisEvent.action)
                    null
                }
            }

            executionResult
        }.toMap()

        processingTimer.record(System.currentTimeMillis() - time, TimeUnit.MILLISECONDS)
        processedChannel.send(state)
    }

}