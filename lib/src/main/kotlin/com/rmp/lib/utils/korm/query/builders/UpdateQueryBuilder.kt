package com.rmp.lib.utils.korm.query.builders

import com.rmp.lib.utils.korm.Row
import com.rmp.lib.utils.korm.Table
import com.rmp.lib.utils.korm.column.EntityId
import com.rmp.lib.utils.korm.column.eq
import com.rmp.lib.utils.korm.query.QueryBuilder
import com.rmp.lib.utils.korm.query.QueryDto
import com.rmp.lib.utils.korm.query.QueryType

class UpdateQueryBuilder(table: Table) : QueryBuilder(table) {

    fun executeRow(row: Row): QueryDto {
        filterExpression = row.columns.firstNotNullOf {
                if (it is EntityId) {
                    it eq row[it]
                } else {
                    null
                }
            }
        return execute(row)
    }

    fun execute(row: Row): QueryDto {
        append("update ")
        append(table.tableName_)
        append(" set ")
        append(row.updatedColumns.joinToString(",") {
            "${it.name}=?"
        }, row.updatedValues)
        loadExpressionFilter()
        return QueryDto.executeQuery(QueryType.UPDATE, getQuery(), getParams())
    }
}