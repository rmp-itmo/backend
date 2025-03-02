package com.rmp.lib.utils.korm.references

import com.rmp.lib.utils.korm.Table
import com.rmp.lib.utils.korm.column.Column

data class Reference (
    val sourceColumn: Column<*>,
    val targetTable: Table,
    val deleteOption: ReferenceOption,
    val updateOption: ReferenceOption
)