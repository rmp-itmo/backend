package com.rmp.lib.utils.korm

import com.rmp.lib.utils.korm.column.Column
import com.rmp.lib.utils.korm.query.QueryBuilder
import com.rmp.lib.utils.korm.query.QueryDto
import com.rmp.lib.utils.korm.query.QueryParseData
import com.rmp.lib.utils.korm.query.QueryType

object TableRegister {
    val tables = mutableMapOf<String, Pair<DbType, Table>>()

    fun register(dbType: DbType, vararg tables: Table) {
        this.tables += tables.associate { it.tableName_ to Pair(dbType, it) }
    }

    fun findColumns(tableName: String, columns: List<String>): List<Column<*>> {
        val (_, table) = tables[tableName] ?: throw Exception("Unknown model $tableName")

        return columns.mapNotNull {
            table.columns[it]
        }
    }

    fun getColumns(queryParseData: QueryParseData): List<Column<*>> =
        queryParseData.map { (tableName, columns) -> findColumns(tableName, columns) }.flatten()
}

fun Table.initTable(dbType: DbType, forceRecreate: Boolean = false): QueryDto {
    val builder = object : QueryBuilder(this) {}.apply {
        setQuery(StringBuilder().apply {
            // Disable for clickhouse due to its incapability to multi statement execution
            if (forceRecreate && dbType != DbType.CLICKHOUSE) {
                append("drop table if exists ")
                append(tableName_)
                if (this@initTable is IdTable)
                    append(" cascade;")
                else
                    append(";")
            }
            append("create table if not exists ")
            append(tableName_)
            append(" (")

            append(
                columns.values.joinToString(separator = ",") { it.ddl(dbType) }
            )

            append(",primary key (")
            append(columns.values.filter { it.type.isPk }.joinToString(separator = ",") { it.name })
            append(")")

            if (this@initTable is IdTable) {
                if (references.isNotEmpty()) {
                    references.forEach { target, refs ->
                        refs.forEach { ref ->
                            append(",foreign key(")
                            append(ref.sourceColumn.name)
                            append(") references ")
                            append(target.tableName_)
                            append("(")
                            append(target.id.name)
                            append(")")
                            append(" on delete ")
                            append(ref.deleteOption.sql)
                            append(" on update ")
                            append(ref.updateOption.sql)
                        }
                    }
                }
            }

            append(");")
        })
    }

    return QueryDto.executeQuery(QueryType.DDL, builder.getQuery())
}
