package com.rmp.lib.utils.korm.query

import com.rmp.lib.utils.korm.Table
import com.rmp.lib.utils.korm.query.builders.filter.Operator

abstract class QueryBuilder(val table: Table) {
    private val params: MutableList<Any?> = mutableListOf()
    private var query: StringBuilder = StringBuilder()
    var filterExpression: Operator? = null

    fun getQuery(): String {
        return query.toString()
    }

    fun getParams(): MutableList<Any?> = params

    fun append(part: String, params: Collection<Any?> = listOf()): QueryBuilder {
        query.append(part)
        this.params.addAll(params)

        return this
    }

    fun setQuery(query: StringBuilder, params: Collection<Any?> = listOf()): QueryBuilder {
        this.query = query
        this.params.addAll(params)

        return this
    }

    protected fun loadExpressionFilter() {
        if (filterExpression != null) {
            val (expression, params) = filterExpression!!.buildExpression()
            append(" WHERE ")
            append(expression, params)
        }
    }
}