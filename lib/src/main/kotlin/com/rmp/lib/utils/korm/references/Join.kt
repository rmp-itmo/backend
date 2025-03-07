package com.rmp.lib.utils.korm.references

import com.rmp.lib.utils.korm.IdTable
import com.rmp.lib.utils.korm.column.eq
import com.rmp.lib.utils.korm.query.QueryBuilder
import com.rmp.lib.utils.korm.query.builders.filter.Operator
import java.sql.SQLException

class Join <T : IdTable>(
    val from: IdTable,
    val target: T,
    val joinType: JoinType,
    val constraints: Operator? = null
) {

    val constraint by lazy {
        (if (constraints == null) {
            val ref = from.references[target]?.firstOrNull() ?: throw SQLException("No reference found for $target in $from")
            ref.sourceColumn eq ref.targetTable.id
        } else constraints).buildExpression()
    }

    fun buildSql(queryBuilder: QueryBuilder): Unit = with(queryBuilder) {
        when (joinType) {
            JoinType.INNER -> {
                append("INNER JOIN ")
            }
            JoinType.LEFT -> {
                append("LEFT JOIN ")
            }
            JoinType.RIGHT -> {
                append("RIGHT JOIN ")
            }
        }

        append(target.tableName_)

        append(" ON ")

        val (expression, params) = constraint

        append(expression, params)
    }
}