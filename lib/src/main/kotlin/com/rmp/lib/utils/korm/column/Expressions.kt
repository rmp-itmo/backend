package com.rmp.lib.utils.korm.column

import com.rmp.lib.utils.korm.query.builders.filter.Operator

infix fun Column<Int>.eq(value: Int): Operator =
    Operator(
        expression = "${this.fullQualifiedName} = ?",
        param = value
    )

infix fun Column<Long>.eq(value: Long): Operator =
    Operator("${this.fullQualifiedName} = ?", value)

@JvmName("eq")
infix fun Column<String>.eq(value: String): Operator =
    Operator("${this.fullQualifiedName} = ?", value)

@JvmName("eq_nullable")
infix fun Column<String?>.eq(value: String): Operator =
    Operator("${this.fullQualifiedName} = ?", value)

infix fun Column<*>.eq(value: Column<*>): Operator =
    Operator("${this.fullQualifiedName} = ${value.fullQualifiedName}")

infix fun Column<Int>.less(value: Int): Operator =
    Operator("${this.fullQualifiedName} < ?", value)

infix fun Column<Long>.less(value: Long): Operator =
    Operator("${this.fullQualifiedName} < ?", value)