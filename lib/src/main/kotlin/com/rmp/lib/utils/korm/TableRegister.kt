package com.rmp.lib.utils.korm

import com.rmp.lib.utils.korm.column.Column
import com.rmp.lib.utils.korm.query.QueryBuilder
import com.rmp.lib.utils.korm.query.QueryDto
import com.rmp.lib.utils.korm.query.QueryParseData
import com.rmp.lib.utils.korm.query.QueryType

object TableRegister {
    val tables = mutableMapOf<String, Table>()

    fun register(vararg tables: Table) {
        this.tables += tables.associateBy { it.tableName_ }
    }

    fun findColumns(tableName: String, columns: List<String>): List<Column<*>> {
        println(tables)
        val table = tables[tableName] ?: throw Exception("Unknown model $tableName")

        return columns.mapNotNull {
            table.columns[it]
        }
    }

    fun getColumns(queryParseData: QueryParseData): List<Column<*>> =
        queryParseData.map { (tableName, columns) -> findColumns(tableName, columns) }.flatten()
}

fun Table.initTable(modifier: StringBuilder.() -> Unit = {}, forceRecreate: Boolean = false): QueryDto {
    val builder = object : QueryBuilder(this) {}.apply {
        setQuery(StringBuilder().apply {
            if (forceRecreate) {
                append("drop table if exists ")
                append(tableName_)
                append(" cascade;")
            }
            append("create table ")
            append(tableName_)
            append(" (")

            append(
                columns.values.joinToString(separator = ",") { it.ddl() }
            )

            append(",primary key (")
            append(columns.values.filter { it.type.isPk }.joinToString(separator = ",") { it.name })
            append(")")

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

            append(");")
        }.apply(modifier))
    }

    return QueryDto.executeQuery(QueryType.DDL, builder.getQuery())
}
