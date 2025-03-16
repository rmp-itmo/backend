package com.rmp.lib.utils.korm

import com.rmp.lib.utils.korm.column.*
import com.rmp.lib.utils.korm.query.*
import com.rmp.lib.utils.korm.query.builders.*
import com.rmp.lib.utils.korm.references.Reference
import com.rmp.lib.utils.korm.references.ReferenceOption


open class IdTable(tableName_: String): Table(tableName_) {
    val id: EntityId = EntityId(this, "id")
    val entityCount: EntityCount = EntityCount(this, "count(*)")

    val references: MutableMap<IdTable, MutableList<Reference>> = mutableMapOf()

    override val columns: MutableMap<String, Column<*>> = mutableMapOf("id" to id, "count(*)" to entityCount)

    fun hasRef(table: Table): Boolean = references.containsKey(table)

    fun reference(name: String, target: IdTable, deleteOption: ReferenceOption = ReferenceOption.RESTRICT, updateOption: ReferenceOption = ReferenceOption.RESTRICT): Column<Long> =
        createColumn(name, LongColumn()).let {
            if (references[target] != null)
                references[target]!! += Reference(it, target, deleteOption, updateOption)
            else
                references[target] = mutableListOf(Reference(it, target, deleteOption, updateOption))
            it
        }

    fun update(row: Row): QueryDto {
        if (row[id] == null) throw Exception("Can`t update row without EntityId")
        return UpdateQueryBuilder(this).executeRow(row)
    }

    fun delete(row: Row) =
        if (row[id] == null) throw Exception("Can`t remove row without EntityId")
        else DeleteQueryBuilder(this).executeRow(row)

}