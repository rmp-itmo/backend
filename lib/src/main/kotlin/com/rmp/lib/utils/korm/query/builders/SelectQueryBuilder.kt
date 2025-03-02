package com.rmp.lib.utils.korm.query.builders

import com.rmp.lib.utils.korm.Table
import com.rmp.lib.utils.korm.column.Column
import com.rmp.lib.utils.korm.query.QueryBuilder
import com.rmp.lib.utils.korm.query.QueryDto
import com.rmp.lib.utils.korm.query.QueryParseData
import com.rmp.lib.utils.korm.references.Join
import com.rmp.lib.utils.korm.references.JoinType

class SelectQueryBuilder(table: Table): QueryBuilder(table) {
    init {
        setQuery(StringBuilder().apply {
            append("SELECT ")
        })
    }

    private var selectColumns: MutableList<Column<*>> = mutableListOf()
    private var joins: MutableList<Join<*>> = mutableListOf()

    fun setColumns(columns: List<Column<*>>): SelectQueryBuilder {
        selectColumns = columns.toMutableList()
        return this
    }

    fun where(filter: FilterExpressionBuilder.() -> Unit): SelectQueryBuilder {
        filterExpression.apply(filter)
        return this
    }

    fun <T: Table> join(
        target: T,
        joinType: JoinType = JoinType.LEFT,
        constraints: (FilterExpressionBuilder.() -> Unit)? = null
    ): SelectQueryBuilder {
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

    fun named(name: String): Pair<String, QueryDto> = Pair(name, execute())

    private fun finalizeQuery(): QueryBuilder {
        setQuery(StringBuilder().apply {
            append("SELECT ")
            if (selectColumns.size >= 1)
                append(selectColumns.joinToString(",") { it.fullQualifiedName })
            else {
                selectColumns += table.columns.values
                selectColumns += joins.map { it.target.columns.values }.flatten()
                append("*")
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

        return QueryDto.selectQuery(getQuery(), queryParseData, getParams())
    }
}