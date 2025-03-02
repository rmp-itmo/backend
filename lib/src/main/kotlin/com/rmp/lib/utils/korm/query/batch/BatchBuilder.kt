package com.rmp.lib.utils.korm.query.batch

import com.rmp.lib.utils.korm.query.*
import com.rmp.lib.utils.korm.query.builders.DeleteQueryBuilder

class BatchBuilder {
    val batch: BatchQuery = BatchQuery()

    @JvmName("addSelectQuery")
    infix fun add (query: Pair<String, QueryDto>) {
        batch += query
    }

    @JvmName("addDeleteQuery")
    infix fun add (query: DeleteQueryBuilder) {
        batch += query.execute() named "delete"
    }

    @JvmName("addQuery")
    infix fun add (query: QueryDto) {
        batch += query named ""
    }

    internal fun init() {
        batch += InitTransactionQueryDto() named "init${batch.queries.size}"
    }

    fun commit() {
        batch += CommitQueryDto() named "commit${batch.queries.size}"
    }

    fun rollback() {
        batch += RollbackQueryDto() named "rollback${batch.queries.size}"
    }
}

fun buildBatch(builder: BatchBuilder.() -> Unit): BatchQuery =
    BatchBuilder().apply(builder).batch

fun newTransaction(builder: BatchBuilder.() -> Unit): BatchQuery =
    BatchBuilder().apply {
        init()
    }.apply(builder).batch

fun autoCommitTransaction(builder: BatchBuilder.() -> Unit): BatchQuery =
    BatchBuilder()
        .apply(builder)
        .apply {
            commit()
        }.batch

fun newAutoCommitTransaction(builder: BatchBuilder.() -> Unit): BatchQuery =
    BatchBuilder()
        .apply {
           init()
        }
        .apply(builder)
        .apply {
            commit()
        }.batch
