package com.rmp.lib.utils.korm

import com.rmp.lib.utils.korm.column.*
import com.rmp.lib.utils.korm.query.*
import com.rmp.lib.utils.korm.query.builders.*
import com.rmp.lib.utils.korm.references.Reference
import com.rmp.lib.utils.korm.references.ReferenceOption


open class Table(val tableName_: String) {
    val id: EntityId = EntityId(this, "id")

    val columns: MutableMap<String, Column<*>> = mutableMapOf("id" to id)

    val references: MutableMap<Table, MutableList<Reference>> = mutableMapOf()

    fun hasRef(table: Table): Boolean = references.containsKey(table)

    private fun <T> createColumn(name: String, columnType: ColumnType<T & Any>): Column<T> {
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

    fun reference(name: String, target: Table, deleteOption: ReferenceOption = ReferenceOption.RESTRICT, updateOption: ReferenceOption = ReferenceOption.RESTRICT): Column<Int> =
        createColumn(name, IntColumn()).let {
            if (references[target] != null)
                references[target]!! += Reference(it, target, deleteOption, updateOption)
            else
                references[target] = mutableListOf(Reference(it, target, deleteOption, updateOption))
            it
        }

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

    fun select(vararg columns: Column<*>): SelectQueryBuilder =
        SelectQueryBuilder(this).setColumns(columns.toList())

    fun update(row: Row): QueryDto {
        if (row[id] == null) throw Exception("Can`t update row without EntityId")
        return UpdateQueryBuilder(this).executeRow(row)
    }

    fun update(filter: FilterExpressionBuilder.() -> Unit, row: Row.() -> Unit) =
        UpdateQueryBuilder(this).apply {
            filterExpression.apply(filter)
        }.execute(Row().apply(row))

    fun delete(row: Row) =
        if (row[id] == null) throw Exception("Can`t remove row without EntityId")
        else DeleteQueryBuilder(this).executeRow(row)

    fun delete(filter: FilterExpressionBuilder.() -> Unit) =
        DeleteQueryBuilder(this).apply {
            filterExpression.apply(filter)
        }.execute()
}

fun <T: Table> T.insert(create: T.(row: Row) -> Unit): QueryDto =
    InsertQueryBuilder(this).execute(Row().apply {
        create(this)
    })