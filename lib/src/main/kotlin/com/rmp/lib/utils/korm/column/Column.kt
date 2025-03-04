package com.rmp.lib.utils.korm.column

import com.rmp.lib.utils.korm.DbType
import com.rmp.lib.utils.korm.Table

open class Column <T> (
    val table: Table,
    val type: ColumnType<T & Any>,
    val name: String
) {
    val fullQualifiedName: String
        get() = "\"${table.tableName_}\".\"$name\""

    private fun T.sqlRepresentation(): String =
        when (this) {
            is String -> "'${this}'"
            is Enum<*> -> this.name
            else -> this.toString()
        }

    private fun sqlRepresentationType(dbType: DbType) =
        type.sqlRepresentationName[dbType] ?: throw Exception("Type $type not implemented for $dbType")

    var defaultValue: T? = null

    fun ddl(dbType: DbType): String = buildString {
        append(name)
        append(" ")
        append(sqlRepresentationType(dbType))
        if (type.nullable) {
            append(" null")
            append(" default ")
            append(defaultValue?.sqlRepresentation())
        } else {
            append(" not null")
            if (defaultValue != null) {
                append(" default ")
                append(defaultValue?.sqlRepresentation())
            }
        }
    }

    override fun equals(other: Any?): Boolean =
        if (other != null && other is Column<*>) {
            other.name == this.name && other.table == this.table
        } else false

    override fun hashCode(): Int {
        var result = table.hashCode()
        result = 31 * result + type.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + (defaultValue?.hashCode() ?: 0)
        return result
    }
}