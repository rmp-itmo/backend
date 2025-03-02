package com.rmp.lib.utils.korm.column

abstract class ColumnType <T> (var nullable: Boolean = false) {
    open var isPk: Boolean = false
    abstract val sqlRepresentationName: String
    abstract fun readValue(value: Any): T
    abstract fun write(value: T): Any
}

class IntColumn: ColumnType<Int>() {
    override val sqlRepresentationName: String
        get() = "int"

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
    override val sqlRepresentationName: String
        get() = "text"

    override fun readValue(value: Any): String =
        when (value) {
            is String -> value
            else -> throw Exception("Unexpected value $value for type Int")
        }

    override fun write(value: String): Any = value
}

sealed class LongImpl: ColumnType<Long>() {
    override val sqlRepresentationName: String
        get() = "bigint"

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
    override val sqlRepresentationName: String
        get() = "bigserial"
}

class EntityIdColumn: LongImpl() {
    override var isPk: Boolean = true
    override val sqlRepresentationName: String
        get() = "bigserial"
}

class FloatColumn: ColumnType<Float>() {
    override val sqlRepresentationName: String
        get() = "float"

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
    override val sqlRepresentationName: String
        get() = "double"

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
    override val sqlRepresentationName: String
        get() = "bool"

    override fun readValue(value: Any): Boolean =
        when (value) {
            is Boolean -> value
            is String -> value.toBoolean()
            else -> throw Exception("Unexpected value $value for type Double")
        }

    override fun write(value: Boolean): String = value.toString()

}