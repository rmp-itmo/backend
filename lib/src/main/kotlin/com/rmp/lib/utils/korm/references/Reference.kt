package com.rmp.lib.utils.korm.references

import com.rmp.lib.utils.korm.IdTable
import com.rmp.lib.utils.korm.column.Column

data class Reference (
    val sourceColumn: Column<*>,
    val targetTable: IdTable,
    val deleteOption: ReferenceOption,
    val updateOption: ReferenceOption,
    val nullable: Boolean = false
)