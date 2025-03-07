package com.rmp.tm.korm

import com.rmp.lib.utils.korm.Row
import com.rmp.lib.utils.korm.RowDto
import com.rmp.lib.utils.korm.TableRegister
import com.rmp.lib.utils.korm.query.*
import com.rmp.lib.utils.redis.RedisEvent
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.actor
import java.sql.Connection
import com.rmp.lib.utils.korm.initTable
import com.rmp.lib.utils.log.Logger

object TransactionManager {
    private lateinit var ds: HikariDataSource

    private val active: MutableMap<String, Connection> = mutableMapOf()

    sealed class QueryEvent(val redisEvent: RedisEvent) {
        class BatchQueryEvent(val query: BatchQuery, redisEvent: RedisEvent) : QueryEvent(redisEvent)
        class SingleQueryEvent(val query: QueryDto, redisEvent: RedisEvent) : QueryEvent(redisEvent)
    }

    val processedChannel: Channel<Pair<RedisEvent, QueryResult>> = Channel(capacity = Channel.BUFFERED)

    @OptIn(ObsoleteCoroutinesApi::class)
    val databaseActor = CoroutineScope(Job()).actor<QueryEvent>(capacity = Channel.BUFFERED) {
        for (e in this) {
            when (e) {
                is QueryEvent.BatchQueryEvent -> {
                    var (connectionName, connection) = getConnection(e.redisEvent.tid)
                    var lastCommitOrRollback = false

                    val executionResult = e.query.queries.mapNotNull { (label, query) ->
                        lastCommitOrRollback = (query.queryType == QueryType.COMMIT.value || query.queryType == QueryType.ROLLBACK.value)

                        Logger.debug("[${QueryType.getByValue(query.queryType)}] ${query.sql} ${query.params}, TID=$connectionName, Conn=$connection", "database")

                        val (tid, conn, executionResult) = processSingleQuery(connectionName, connection, query)

                        connectionName = tid
                        connection = conn

                        Logger.debug("Result: $executionResult; \n CurTid=$tid \n CurConn=$conn", "database")

                        if (executionResult == null) null
                        else label to executionResult
                    }.toMap()

                    // При обработке commit и rollback запросов автоматически генерируется новое подключение, чтобы
                    // обработка дальнейших запросов автоматически продолжалось и не пришлось каждый раз после коммита
                    // в ручную инициализировать подключение
                    // Из-за такого поведения, может возникнуть ситуация, когда commit стоит последним, но временное подключение
                    // все равно создалось, в этом случае его надо убить.
                    if (lastCommitOrRollback && connectionName != null && connection != null) {
                        commit(connectionName!!, connection!!)
                        connectionName = null
                        connection = null
                    }
                    // Обратная ситуация, когда мы не остановились на коммите (или даже если их вообще не было и мы работали в одном контексте)
                    // Следует дописать в active текущее подключение, чтобы пользователю не улетел tid которого на самом деле нет.
                    else if (!lastCommitOrRollback && connectionName != null && connection != null) {
                        active += Pair(connectionName!!, connection!!)
                    }

                    if (executionResult.isNotEmpty())
                        processedChannel.send(Pair(e.redisEvent, QueryResult(
                            connectionName,
                            executionResult
                        )))
                }
                is QueryEvent.SingleQueryEvent -> {
                    val (name, connection) = getConnection(e.redisEvent.tid)

                    if (connection == null) continue

                    val (tidAfterProcess, _, executionResult) = processSingleQuery(name, connection, e.query)

                    // Если на момент выполнения не было открыто подключения,
                    // get connection вернет пустое null имя и временное подключение connection
                    // В этом случае после выполнения запроса мы автоматически закрываем это подключение
                    if (name == null && !connection.isClosed)
                        connection.commit()

                    if (executionResult != null) {
                        processedChannel.send(Pair(e.redisEvent, QueryResult(
                            tidAfterProcess,
                            mutableMapOf("" to executionResult)
                        )))
                    }
                }
            }
        }
    }

    private fun getConnection(name: String?): Pair<String?, Connection?> {
        if (name != null)
            return Pair(name, active[name])

        val entry = Pair(null, ds.connection)
        return entry
    }

    private fun processSingleQuery(connectionName: String?, connection: Connection?, query: QueryDto): Triple<String?, Connection?, ExecutionResult?> {
        return when (QueryType.getByValue(query.queryType)) {
            QueryType.INIT -> {
                val result = listOf(RowDto(mutableMapOf("result" to ResultPlaceholder.INITIALIZED.value)))

                val conn = if (connectionName == null) {
                    if (connection != null) {
                        // ОЧЕНЬ ВАЖНЫЙ ЛОГ!
                        Logger.debug("CLOSE TEMP-CONNECTION! $connection", "database")
                        connection.commit()
                        connection.close()
                    }
                    val newConnection = ds.connection
                    Logger.debug("OPEN NEW CONNECTION $newConnection", "database")
                    val new = Pair("tid-${System.nanoTime()}", newConnection)
                    active += new
                    new
                } else {
                    getConnection(connectionName)
                }

                Triple(conn.first, conn.second, ExecutionResult(mutableMapOf(), result))
            }

            QueryType.SELECT, QueryType.INSERT -> {
                if (connection == null) return Triple(null, null, null)
                Triple(
                    connectionName,
                    connection,
                    ExecutionResult(
                        query.queryParseData ?: mutableMapOf(),
                        execute(
                            connection,
                            query,
                            query.queryParseData?.values?.flatten() ?: listOf(),
                            query.queryType == QueryType.INSERT.value
                        )
                    )
                )
            }
            QueryType.UPDATE, QueryType.DELETE -> {
                if (connection == null) return Triple(null, null, null)
                Triple(
                    connectionName,
                    connection,
                    ExecutionResult(mutableMapOf(), executeNoResult(connection, query))
                )
            }

            QueryType.COMMIT -> {
                if (connectionName == null || connection == null) Triple(null, null, null)
                else {
                    val (name, tempConnection, _) = processSingleQuery(null, null, InitTransactionQueryDto())
                    Triple(
                        name,
                        tempConnection,
                        ExecutionResult(mutableMapOf(), commit(connectionName, connection))
                    )
                }
            }

            QueryType.ROLLBACK -> {
                if (connectionName == null || connection == null) Triple(null, null, null)
                else {
                    val (name, tempConnection, _) = processSingleQuery(null, null, InitTransactionQueryDto())
                    Triple(
                        name,
                        tempConnection,
                        ExecutionResult(mutableMapOf(), rollback(connectionName, connection))
                    )
                }
            }

            else -> {
                Logger.debug("Unknown query type ${query.queryType}", "database")
                Triple(null, null, null)
            }
        }
    }

    private fun commit(name: String, connection: Connection): List<RowDto> {
        Logger.debug("COMMIT AND CLOSE THE CONNECTION $connection", "database")
        connection.commit()
        connection.close()
        active.remove(name)

        return listOf(RowDto(mutableMapOf("result" to ResultPlaceholder.COMMITED.value)))
    }

    private fun rollback(name: String, connection: Connection): List<RowDto> {
        Logger.debug("ROLLBACK AND CLOSE THE CONNECTION $connection", "database")
        connection.rollback()
        connection.close()
        active.remove(name)

        return listOf(RowDto(mutableMapOf("result" to ResultPlaceholder.ROLLEDBACK.value)))
    }

    private fun execute(connection: Connection, query: QueryDto, columns: List<String>, insert: Boolean = false): List<RowDto> {
        val start = System.currentTimeMillis()

        val stmt = query.prepare(connection)

        val rs = if (!insert) stmt.executeQuery()
                 else {
                    stmt.executeUpdate()
                    stmt.generatedKeys
                 }

        val result = mutableListOf<RowDto>()
        while (rs.next()) {
            result += RowDto.build(rs, columns)
        }

        Logger.debug("Execution time: ${System.currentTimeMillis() - start} (${query.sql})", "database")

        return result
    }

    private fun executeNoResult(connection: Connection, query: QueryDto): List<RowDto> {
        val stmt = query.prepare(connection)

        val result = stmt.executeUpdate()

        return listOf(RowDto(mutableMapOf("result" to result)))
    }

    fun init(config: HikariConfig.() -> Unit) {
        ds = HikariDataSource(HikariConfig().apply(config).apply {
            isAutoCommit = false
        })
    }

    fun initTables(forceRecreate: Boolean) {
        val connection = ds.connection
        TableRegister.tables.forEach { (_, entry) ->
            val (type, table) = entry
            val query = table.initTable(dbType = type, forceRecreate = forceRecreate)
            Logger.debug("Init table ${table.tableName_} - ${query.sql}", "database")
            executeNoResult(connection, query)
        }
        connection.commit()
        connection.close()
    }
}