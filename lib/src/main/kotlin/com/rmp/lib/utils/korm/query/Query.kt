package com.rmp.lib.utils.korm.query

import com.rmp.lib.utils.korm.RowDto
import com.rmp.lib.utils.korm.query.batch.BatchEntry
import com.rmp.lib.utils.redis.SerializableClass
import com.rmp.lib.utils.redis.UuidSerializer
import com.rmp.lib.utils.serialization.UltimateSerializer
import kotlinx.serialization.Serializable
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.SQLException
import java.sql.Statement
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

sealed class Query: SerializableClass

enum class ResultPlaceholder(val value: Int) {
    INITIALIZED(-1),
    COMMITED(-2),
    ROLLEDBACK(-3),
    CLOSED(-4)
}

enum class QueryType(val value: Int) {
    DDL(0),
    INIT(1),
    SELECT(2),
    UPDATE(3),
    DELETE(3),
    INSERT(4),
    COMMIT(5),
    ROLLBACK(6);

    companion object {
        fun getByValue(value: Int): QueryType =
            QueryType.entries.first { it.value == value }
    }
}

// Table -> List of Columns
typealias QueryParseData = MutableMap<String, MutableList<String>>

@Serializable
data class ExecutionResult(
    val parseData: QueryParseData,
    val rows: List<RowDto>,
) {
    companion object {
        fun empty(): ExecutionResult = ExecutionResult(mutableMapOf(), listOf())
    }
}

@Serializable
data class QueryResult (
    val result: Map<String, ExecutionResult>
): SerializableClass

@Serializable
open class QueryDto (
    val queryType: Int, // 1 - Initialization, 2 - Common sql, 3 - commit, 4 - rollback
    val sql: String,
    val params: List<@Serializable(with= UltimateSerializer::class) Any?> = listOf(),
    val queryParseData: QueryParseData? = null,
): Query() {
    companion object {
        fun executeQuery(queryType: QueryType, sqlQuery: String, params: List<Any?> = listOf(), queryParseData: QueryParseData? = null): QueryDto =
            QueryDto(queryType.value, sqlQuery, params, queryParseData)

        fun selectQuery(sqlQuery: String, queryParseData: QueryParseData, params: List<Any?> = listOf()): QueryDto =
            QueryDto(QueryType.SELECT.value, sqlQuery, params, queryParseData)
    }

    infix fun named(name: String): Pair<String, QueryDto> {
        return Pair(name, this)
    }

    fun prepare(connection: Connection): PreparedStatement {
        val stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)
        params.forEachIndexed { index, param ->
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

        return stmt
    }
}

@Serializable
class InitTransactionQueryDto: QueryDto(QueryType.INIT.value, "", listOf())

@Serializable
data class BatchQuery(
    val queries: MutableMap<String, QueryDto> = mutableMapOf()
): Query() {
    operator fun plusAssign(batchEntry: BatchEntry) {
        queries += batchEntry
    }
}

@Serializable
class CommitQueryDto: QueryDto(QueryType.COMMIT.value, "", listOf())

@Serializable
class RollbackQueryDto: QueryDto(QueryType.ROLLBACK.value, "", listOf())

fun Pair<String, QueryDto>.named(name: String): Pair<String, QueryDto> {
    return name to second
}