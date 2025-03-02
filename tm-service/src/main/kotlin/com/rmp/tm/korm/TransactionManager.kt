package com.rmp.tm.korm

import com.rmp.lib.utils.korm.RowDto
import com.rmp.lib.utils.korm.Table
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
import java.sql.PreparedStatement
import java.sql.SQLException
import com.rmp.lib.utils.korm.initTable

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
                    var (name, connection) = getConnection(e.redisEvent.tid)
                    var lastCommitOrRollback = false

                    val executionResult = e.query.queries.mapNotNull { (label, query) ->
                        lastCommitOrRollback = (query.queryType == QueryType.COMMIT.value || query.queryType == QueryType.ROLLBACK.value)

                        val (tid, conn, executionResult) = processSingleQuery(name, connection, query)

                        name = tid
                        connection = conn

                        println("${query.queryType} ${query.sql} ${query.params}")
                        println("Result: $executionResult")

                        if (executionResult == null) null
                        else label to executionResult
                    }.toMap()

                    // При обработке commit и rollback запросов автоматически генерируется новое подключение, чтобы
                    // обработка дальнейших запросов автоматически продолжалось и не пришлось каждый раз после коммита
                    // в ручную инициализировать подключение
                    // Из-за такого поведения, может возникнуть ситуация, когда commit стоит последним, но временное подключение
                    // все равно создалось, в этом случае его надо убить.
                    if (lastCommitOrRollback && name != null && connection != null) {
                        commit(name!!, connection!!)
                    }
                    // Обратная ситуация, когда мы не остановились на коммите (или даже если их вообще не было и мы работали в одном контексте)
                    // Следует дописать в active текущее подключение, чтобы пользователю не улетел tid которого на самом деле нет.
                    else if (!lastCommitOrRollback && name != null && connection != null) {
                        active += Pair(name!!, connection!!)
                    }

                    if (executionResult.isNotEmpty())
                        processedChannel.send(Pair(e.redisEvent, QueryResult(
                            name,
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
        return when (query.queryType) {
            QueryType.INIT.value -> {
                val result = listOf(RowDto(mutableMapOf("result" to ResultPlaceholder.INITIALIZED.value)))

                val conn = if (connectionName == null) {
                    if (connection != null) {
                        // ОЧЕНЬ ВАЖНЫЙ ЛОГ!
                        println("CLOSE TEMP-CONNECTION! $connection")
                        connection.commit()
                        connection.close()
                    }
                    val newConnection = ds.connection
                    println("OPEN NEW CONNECTION $newConnection")
                    val new = Pair("tid-${System.nanoTime()}", newConnection)
                    active += new
                    new
                } else {
                    getConnection(connectionName)
                }

                Triple(conn.first, conn.second, ExecutionResult(mutableMapOf(), result))
            }

            QueryType.SELECT.value -> {
                println(connectionName)
                println(connection)
                if (connection == null) return Triple(null, null, null)
                Triple(
                    connectionName,
                    connection,
                    ExecutionResult(
                        query.queryParseData ?: mutableMapOf(),
                        execute(
                            connection,
                            query,
                            query.queryParseData?.values?.flatten() ?: listOf()
                        )
                    )
                )
            }

            QueryType.UPDATE.value or QueryType.DELETE.value or QueryType.INSERT.value -> {
                if (connection == null) return Triple(null, null, null)
                Triple(
                    connectionName,
                    connection,
                    ExecutionResult(mutableMapOf(), executeNoResult(connection, query))
                )
            }

            QueryType.COMMIT.value -> {
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

            QueryType.ROLLBACK.value -> {
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
                Triple(null, null, null)
            }
        }
    }

    private fun commit(name: String, connection: Connection): List<RowDto> {
        println("COMMIT AND CLOSE THE CONNECTION $connection")
        connection.commit()
        connection.close()
        active.remove(name)

        return listOf(RowDto(mutableMapOf("result" to ResultPlaceholder.COMMITED.value)))
    }

    private fun rollback(name: String, connection: Connection): List<RowDto> {
        println("ROLLBACK AND CLOSE THE CONNECTION $connection")
        connection.rollback()
        connection.close()
        active.remove(name)

        return listOf(RowDto(mutableMapOf("result" to ResultPlaceholder.ROLLEDBACK.value)))
    }

    private fun prepare(connection: Connection, query: QueryDto): PreparedStatement {
        val stmt = connection.prepareStatement(query.sql)
        query.params.forEachIndexed { index, param ->
            if (param == null) {
                stmt.setObject(index + 1, null)
                return@forEachIndexed
            }

            when(param) {
                is Int -> stmt.setInt(index + 1, param)
                is Long -> stmt.setLong(index + 1, param)
                is String -> stmt.setString(index + 1, param)
                is Boolean -> stmt.setBoolean(index + 1, param)
                is Float -> stmt.setFloat(index + 1, param)
                is Double -> stmt.setDouble(index + 1, param)
                else -> throw SQLException("Unknown param type $param")
            }
        }

        println("On execute: $stmt")
//        println(queryBuilder.getQuery())
//        println(queryBuilder.getParams())
//        println(stmt.connection)
        return stmt
    }

    private fun execute(connection: Connection, query: QueryDto, columns: List<String>): List<RowDto> {
        val start = System.currentTimeMillis()
        println("Execution started: $start")

        val stmt = prepare(connection, query)

        val rs = stmt.executeQuery()

        val result = mutableListOf<RowDto>()
        while (rs.next()) {
            result += RowDto.build(rs, columns)
        }

        println("Execution finished ${System.currentTimeMillis() - start}")

        return result
    }

    private fun executeNoResult(connection: Connection, query: QueryDto): List<RowDto> {
        val stmt = prepare(connection, query)

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
        TableRegister.tables.forEach {
            executeNoResult(connection, it.value.initTable(forceRecreate = forceRecreate))
        }
        connection.commit()
        connection.close()
    }
}