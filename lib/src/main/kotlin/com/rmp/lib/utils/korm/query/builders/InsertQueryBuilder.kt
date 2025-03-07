package com.rmp.lib.utils.korm.query.builders

import com.rmp.lib.utils.korm.IdTable
import com.rmp.lib.utils.korm.Row
import com.rmp.lib.utils.korm.Table
import com.rmp.lib.utils.korm.query.QueryBuilder
import com.rmp.lib.utils.korm.query.QueryDto
import com.rmp.lib.utils.korm.query.QueryType

class InsertQueryBuilder(table: Table) : QueryBuilder(table) {
    fun execute(row: Row): QueryDto {
        append("insert into ")
        append(table.tableName_)
        append("(")
        append(row.columns.joinToString(",") { it.name })
        append(") values (")
        append(row.columns.joinToString(",") {
            "?"
        }, row.values)
        append(")")

        return if (table is IdTable)
            QueryDto.executeQuery(QueryType.INSERT, getQuery(), getParams(), mutableMapOf(table.tableName_ to mutableListOf(table.id.name)))
        else
            QueryDto.executeQuery(QueryType.INSERT, getQuery(), getParams(), mutableMapOf(table.tableName_ to mutableListOf()))
    }
}