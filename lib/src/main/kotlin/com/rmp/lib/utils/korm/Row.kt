package com.rmp.lib.utils.korm

import com.rmp.lib.utils.korm.column.Column
import java.sql.ResultSet

class Row private constructor() {
    private val data: MutableMap<Column<*>, Any?> = mutableMapOf()
    private val updated: MutableSet<Column<*>> = mutableSetOf()

    val columns: Set<Column<*>>
        get() = data.keys

    val values: Collection<Any?>
        get() = data.values

    val updatedColumns: Set<Column<*>>
        get() = updated

    val updatedValues: List<Any?>
        get() = updatedColumns.map { data[it] }

    companion object {
        fun apply(row: Row.() -> Unit): Row {
            return Row().apply(row)
        }

        fun build(rs: ResultSet, columns: List<Column<*>>): Row {
            val row = Row()
            columns.forEachIndexed { index, column ->
                row.data[column] = rs.getObject(index + 1)
            }
            return row
        }

        fun build(rowDto: RowDto, columns: List<Column<*>>): Row {
            val row = Row()

            columns.forEachIndexed { index, column ->
                row.data[column] = rowDto.serializedData[column.name]
            }

            return row
        }
    }

    operator fun <T> get(column: Column<T>): T {
        if (!data.containsKey(column))
            throw Exception("Unknown column")
        return data[column]?.let { column.type.readValue(it) } ?: throw Exception("Unknown column")
    }

    operator fun <T> set(column: Column<T>, value: T?) {
        updated += column
        data[column] = value
    }

    fun unwrap(table: Table): Row {
        val unwrapped = Row()
        table.columns.values.forEach {
            unwrapped.data[it] = data[it]
        }

        return unwrapped
    }

    override fun toString(): String {
        return "Row {\n\tdata=${data.map { 
            Pair(it.key.name, it.value)
        }.toMap()}\n\tupdated=${updated.map { it.name }}\n}"
    }
}