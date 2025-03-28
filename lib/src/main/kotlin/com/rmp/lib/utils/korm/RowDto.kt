package com.rmp.lib.utils.korm

import com.rmp.lib.utils.korm.query.QueryParseData
import com.rmp.lib.utils.log.Logger
import com.rmp.lib.utils.redis.SerializableClass
import com.rmp.lib.utils.serialization.UltimateSerializer
import kotlinx.serialization.Serializable
import java.sql.ResultSet

@Serializable
data class RowDto (
    val serializedData: MutableMap<String, @Serializable(with = UltimateSerializer::class) Any?> = mutableMapOf(),
): SerializableClass {
    companion object {
        fun build(rs: ResultSet, columns: List<String>): RowDto {
            val row = RowDto()
            Logger.debug("BUILD ROW DTO OBJECT: $columns")
            columns.forEachIndexed { index, column ->
                row.serializedData[column] = rs.getObject(index + 1)
            }
            return row
        }
    }

    fun toRow(parseData: QueryParseData): Row {
        val columns = parseData.map {
            TableRegister.findColumns(it.key, it.value)
        }.flatten()

        return Row.build(this, columns)
    }
}