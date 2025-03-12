package com.rmp.lib.utils.korm.query.builders

import com.rmp.lib.utils.korm.IdTable
import com.rmp.lib.utils.korm.Table
import com.rmp.lib.utils.korm.column.Column
import com.rmp.lib.utils.korm.column.EntityCount
import com.rmp.lib.utils.korm.query.QueryBuilder
import com.rmp.lib.utils.korm.query.QueryDto
import com.rmp.lib.utils.korm.query.QueryParseData
import com.rmp.lib.utils.korm.query.builders.filter.Operator
import com.rmp.lib.utils.korm.query.builders.filter.and
import com.rmp.lib.utils.korm.query.builders.filter.or
import com.rmp.lib.utils.korm.references.Join
import com.rmp.lib.utils.korm.references.JoinType

class SelectQueryBuilder<T: Table>(table: T): QueryBuilder(table) {
    init {
        setQuery(StringBuilder().apply {
            append("SELECT ")
        })
    }

    private var selectColumns: MutableList<Column<*>> = mutableListOf()
    private var joins: MutableList<Join<*>> = mutableListOf()
    private var limit: Long? = null
    private var offset: Long? = null
    private var countQuery: Boolean = false

    fun setColumns(columns: List<Column<*>>): SelectQueryBuilder<T> {
        selectColumns = columns.toMutableList()
        return this
    }

    fun where(filter: () -> Operator): SelectQueryBuilder<T> {
        if (filterExpression != null) throw Exception("Duplicate where clause (use 'andWhere' or 'orWhere' methods instead)")
        filterExpression = filter.invoke()
        return this
    }

    fun andWhere(filter: () -> Operator): SelectQueryBuilder<T> {
        filterExpression = filterExpression and filter.invoke()
        return this
    }

    fun orWhere(filter: () -> Operator): SelectQueryBuilder<T> {
        filterExpression = filterExpression or filter.invoke()
        return this
    }

    fun limit(n: Number): SelectQueryBuilder<T> {
        limit = n.toLong()
        return this
    }

    fun offset(n: Number): SelectQueryBuilder<T> {
        offset = n.toLong()
        return this
    }

    fun <R: IdTable> join(
        target: R,
        joinType: JoinType = JoinType.LEFT,
        constraints: Operator? = null
    ): SelectQueryBuilder<T> {
        if (table !is IdTable) {
            throw Exception("Try joining with table without id")
        }
        joins.asReversed().forEach {
            if (it.target.hasRef(target)) {
                joins += Join(it.target, target, joinType, constraints)
                return@join this
            }
        }
        if (table.hasRef(target)) {
            joins += Join(table, target, joinType, constraints)
            return this
        }

        throw Exception("No reference found for ${target.tableName_} in current query")
    }


    fun count(name: String): Pair<String, QueryDto> {
        countQuery = true
        return Pair(name, execute())
    }

    fun named(name: String): Pair<String, QueryDto> = Pair(name, execute())

    private fun finalizeQuery(): QueryBuilder {
        setQuery(StringBuilder().apply {
            append("SELECT ")
            if (countQuery) {
                append("COUNT(*)")
                selectColumns = mutableListOf()
            } else {
                if (selectColumns.size >= 1)
                    append(selectColumns.joinToString(",") { it.fullQualifiedName })
                else {
                    selectColumns += table.columns.values
                    selectColumns += joins.map { tableJoin -> tableJoin.target.columns.filterNot { it.value is EntityCount }.values }.flatten()
                    append("*")
                }
            }
            append(" FROM ")
            append(table.tableName_)
        })

        append(" ")

        joins.forEach {
            it.buildSql(this)
        }

        append(" ")

        loadExpressionFilter()

        append(" ")

        if (limit != null)
            append("LIMIT $limit")

        append(" ")

        if (offset != null)
            append("OFFSET $offset")

        return this
    }

    private fun execute(): QueryDto {
        finalizeQuery()

        val queryParseData: QueryParseData = mutableMapOf()

        selectColumns.forEach {
            if (queryParseData.containsKey(it.table.tableName_))
                queryParseData[it.table.tableName_]!! += it.name
            else
                queryParseData[it.table.tableName_] = mutableListOf(it.name)
        }

        if (countQuery && table is IdTable)
            queryParseData[table.tableName_] = mutableListOf(table.entityCount.name)

        return QueryDto.selectQuery(getQuery(), queryParseData, getParams())
    }
}