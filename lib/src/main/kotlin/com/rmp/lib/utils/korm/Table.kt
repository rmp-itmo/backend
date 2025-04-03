package com.rmp.lib.utils.korm

import com.rmp.lib.utils.korm.column.*
import com.rmp.lib.utils.korm.query.*
import com.rmp.lib.utils.korm.query.builders.*
import com.rmp.lib.utils.korm.query.builders.filter.Operator


open class Table(val tableName_: String) {
    open val columns: MutableMap<String, Column<*>> = mutableMapOf()

    protected fun <T> createColumn(name: String, columnType: ColumnType<T & Any>): Column<T> {
        val col = Column<T>(this, columnType, name)
        columns += Pair(col.name, col)
        return col
    }

    fun int(name: String): Column<Int> = createColumn(name, IntColumn())

    fun long(name: String): Column<Long> = createColumn(name, LongColumn())

    fun float(name: String): Column<Float> = createColumn(name, FloatColumn())

    fun double(name: String): Column<Double> = createColumn(name, DoubleColumn())

    fun text(name: String): Column<String> = createColumn(name, TextColumn())

    fun bool(name: String): Column<Boolean> = createColumn(name, BoolColumn())

    fun autoInc(name: String): Column<Long> = createColumn(name, SerialColumn())

    fun dateTime(name: String): Column<String> = createColumn(name, DateTimeColumn())

    fun <T> Column<T>.nullable(): Column<T?> {
        val col = Column<T?>(this@Table, type, name).apply {
            type.nullable = true
        }
        columns[col.name] = col
        return col
    }

    fun <T> Column<T>.default(value: T?): Column<T> {
        this.defaultValue = value
        return this
    }

    fun <T> Column<T>.pk(): Column<T> {
        this.type.isPk = true
        return this
    }

    fun select(vararg columns: Column<*>): SelectQueryBuilder<*> =
        SelectQueryBuilder(this).setColumns(columns.toList())

    fun update(filter: Operator, row: Row.() -> Unit) =
        UpdateQueryBuilder(this).apply {
            filterExpression = filter
        }.execute(Row.build(row))

    fun delete(filter: Operator) =
        DeleteQueryBuilder(this).apply {
            filterExpression = filter
        }.execute().named("delete-$tableName_")

    fun <T> batchInsert(collection: Collection<T>, processor: Row.(item: T, idx: Int) -> Unit): QueryDto =
        InsertQueryBuilder(this).execute(collection.mapIndexed { idx, it ->
            val row = Row.build{}
            processor.invoke(row, it, idx)
            row
        })
}

fun <T: Table> T.insert(create: T.(row: Row) -> Unit): QueryDto =
    InsertQueryBuilder(this).execute(Row.build {
        create(this)
    })