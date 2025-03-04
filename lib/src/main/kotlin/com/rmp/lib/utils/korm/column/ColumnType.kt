package com.rmp.lib.utils.korm.column

import com.rmp.lib.utils.korm.DbType

abstract class ColumnType <T> (var nullable: Boolean = false) {
    open var isPk: Boolean = false
    // DB Name -> Type definition
    abstract val sqlRepresentationName: MutableMap<DbType, String>
    abstract fun readValue(value: Any): T
    abstract fun write(value: T): Any
}

class IntColumn: ColumnType<Int>() {
    override val sqlRepresentationName: MutableMap<DbType, String>
        get() = mutableMapOf(
            DbType.PGSQL to "int",
            DbType.CLICKHOUSE to "Int32"
        )

    override fun readValue(value: Any): Int =
        when (value) {
            is Int -> value
            is Number -> value.toInt()
            is String -> value.toInt()
            else -> throw Exception("Unexpected value $value for type Int")
        }

    override fun write(value: Int): Any = value
}

class TextColumn: ColumnType<String>() {
    override val sqlRepresentationName: MutableMap<DbType, String>
        get() = mutableMapOf(
            DbType.PGSQL to "text",
            DbType.CLICKHOUSE to "String"
        )

    override fun readValue(value: Any): String =
        when (value) {
            is String -> value
            else -> throw Exception("Unexpected value $value for type Int")
        }

    override fun write(value: String): Any = value
}

sealed class LongImpl: ColumnType<Long>() {
    override val sqlRepresentationName: MutableMap<DbType, String>
        get() = mutableMapOf(
            DbType.PGSQL to "bigint",
            DbType.CLICKHOUSE to "Int64"
        )

    override fun readValue(value: Any): Long =
        when (value) {
            is Long -> value
            is Number -> value.toLong()
            is String -> value.toLong()
            else -> throw Exception("Unexpected value $value for type Long")
        }

    override fun write(value: Long): Any = value
}

class LongColumn: LongImpl()

class SerialColumn: LongImpl() {
    override val sqlRepresentationName: MutableMap<DbType, String>
        get() = mutableMapOf(
            DbType.PGSQL to "bigint",
            // DbType.CLICKHOUSE not implemented
        )
}

class EntityIdColumn: LongImpl() {
    override var isPk: Boolean = true
    override val sqlRepresentationName: MutableMap<DbType, String>
        get() = mutableMapOf(
            DbType.PGSQL to "bigint"
            // DbType.CLICKHOUSE not implemented
        )
}

class FloatColumn: ColumnType<Float>() {
    override val sqlRepresentationName: MutableMap<DbType, String>
        get() = mutableMapOf(
            DbType.PGSQL to "float",
            DbType.CLICKHOUSE to "Float32"
        )

    override fun readValue(value: Any): Float =
        when (value) {
            is Float -> value
            is Number -> value.toFloat()
            is String -> value.toFloat()
            else -> throw Exception("Unexpected value $value for type Float")
        }

    override fun write(value: Float): Any = value

}

class DoubleColumn: ColumnType<Double>() {
    override val sqlRepresentationName: MutableMap<DbType, String>
        get() = mutableMapOf(
            DbType.PGSQL to "double",
            DbType.CLICKHOUSE to "Float64"
        )

    override fun readValue(value: Any): Double =
        when (value) {
            is Double -> value
            is Number -> value.toDouble()
            is String -> value.toDouble()
            else -> throw Exception("Unexpected value $value for type Double")
        }

    override fun write(value: Double): String = value.toString()

}

class BoolColumn: ColumnType<Boolean>() {
    override val sqlRepresentationName: MutableMap<DbType, String>
        get() = mutableMapOf(
            DbType.PGSQL to "bool",
            DbType.CLICKHOUSE to "bool"
        )

    override fun readValue(value: Any): Boolean =
        when (value) {
            is Boolean -> value
            is String -> value.toBoolean()
            else -> throw Exception("Unexpected value $value for type Double")
        }

    override fun write(value: Boolean): String = value.toString()

}