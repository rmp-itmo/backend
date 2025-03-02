package com.rmp.lib.utils.korm.references

import com.rmp.lib.utils.korm.Table
import com.rmp.lib.utils.korm.query.builders.FilterExpressionBuilder
import com.rmp.lib.utils.korm.query.QueryBuilder
import java.sql.SQLException

class Join <T : Table>(
    val from: Table,
    val target: T,
    val joinType: JoinType,
    constraint: (FilterExpressionBuilder.() -> Unit)? = null
) {
    val expressionBuilder by lazy {
        FilterExpressionBuilder().apply(
            if (constraint == null) {
                val ref = from.references[target]?.firstOrNull() ?: throw SQLException("No reference found for $target in $from");
                {
                    ref.sourceColumn eq ref.targetTable.id
                }
            } else constraint
        )
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

        append(expressionBuilder.expressions.joinToString(" "), expressionBuilder.params)
    }
}