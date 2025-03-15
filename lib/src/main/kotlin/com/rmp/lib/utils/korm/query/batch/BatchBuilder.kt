package com.rmp.lib.utils.korm.query.batch

import com.rmp.lib.utils.korm.query.*
import com.rmp.lib.utils.korm.query.builders.DeleteQueryBuilder
import com.rmp.lib.utils.korm.query.builders.SelectQueryBuilder

typealias BatchEntry = Pair<String, QueryDto>

class BatchBuilder {
    val batch: BatchQuery = BatchQuery()


    @JvmName("addSelectQuery")
    infix fun add (query: BatchEntry) {
        batch += query
    }

    infix fun add (query: SelectQueryBuilder<*>) {
        batch += query.named(query.table.tableName_)
    }

    @JvmName("addDeleteQuery")
    infix fun add (query: DeleteQueryBuilder) {
        batch += query.execute() named "delete-${query.table.tableName_}"
    }

    internal fun init() {
        batch += InitTransactionQueryDto() named "init-${batch.queries.size}"
    }

    fun commit() {
        batch += CommitQueryDto() named "commit-${batch.queries.size}"
    }

    fun rollback() {
        batch += RollbackQueryDto() named "rollback-${batch.queries.size}"
    }

    companion object {
        fun build(builder: BatchBuilder.() -> Unit): BatchQuery =
            BatchBuilder().apply(builder).batch
        fun buildAutoCommit(builder: BatchBuilder.() -> Unit): BatchQuery =
            BatchBuilder().apply{init()}.apply(builder).apply { commit() }.batch
    }
}