package com.rmp.lib.utils.korm.query.builders

import com.rmp.lib.utils.korm.IdTable
import com.rmp.lib.utils.korm.Row
import com.rmp.lib.utils.korm.Table
import com.rmp.lib.utils.korm.column.EntityId
import com.rmp.lib.utils.korm.column.eq
import com.rmp.lib.utils.korm.query.QueryBuilder
import com.rmp.lib.utils.korm.query.QueryDto
import com.rmp.lib.utils.korm.query.QueryType
import com.rmp.lib.utils.log.Logger

class UpdateQueryBuilder(table: Table) : QueryBuilder(table) {

    fun executeRow(row: Row): Pair<String, QueryDto> {
        filterExpression = row.columns.firstNotNullOf {
                if (it is EntityId) {
                    it eq row[it]
                } else {
                    null
                }
            }
        return execute(row)
    }

    fun execute(row: Row): Pair<String, QueryDto> {
        append("update ")
        append(table.tableName_)
        append(" set ")
        append(row.updatedColumns.joinToString(",") {
            "${it.name}=?"
        }, row.updatedValues)
        append(row.incremented.joinToString(",") { (col, type, _) ->
            "${col.name} = ${col.name} ${if (type == Row.UpdateOp.INC) "+" else "-"} ?"
        }, row.incremented.map{(_, _, v) -> v})
        loadExpressionFilter()

        val queryParseData = with(mutableMapOf<String, MutableList<String>>()) {
            if (table is IdTable) {
                this += (table.tableName_ to mutableListOf(table.updateCount.name))
            }
            this
        }

        Logger.debug(queryParseData.toString())

        return QueryDto.executeQuery(QueryType.UPDATE, getQuery(), getParams(), queryParseData).named(table.tableName_)
    }
}