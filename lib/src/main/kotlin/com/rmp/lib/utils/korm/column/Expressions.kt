package com.rmp.lib.utils.korm.column

import com.rmp.lib.utils.korm.query.builders.filter.Operator
import com.rmp.lib.utils.korm.query.builders.filter.and

fun <T> Column<T?>.isNull(): Operator =
    Operator("${this.fullQualifiedName} is NULL")

infix fun <T: Number, R: Number> Column<T>.inList(value: List<R>): Operator =
    if (value.isEmpty())
        Operator("${this.fullQualifiedName} != ${this.fullQualifiedName}")
    else
        Operator("${this.fullQualifiedName} in (${value.joinToString(",") {"?"}})", value)

infix fun <T: Number, R: Number> Column<T>.notInList(value: List<R>): Operator =
    if (value.isEmpty())
        Operator("${this.fullQualifiedName} = ${this.fullQualifiedName}")
    else
        Operator("${this.fullQualifiedName} not in (${value.joinToString(",") {"?"}})", value)

@JvmName("numberEq")
infix fun <T: Number, R: Number> Column<T>.eq(value: R): Operator =
    Operator("${this.fullQualifiedName} = ?", value)

@JvmName("numberEqNullable")
infix fun <T: Number, R: Number> Column<T?>.eq(value: R): Operator =
    Operator("${this.fullQualifiedName} = ?", value)

infix fun <T: Number, R: Number> Column<T>.neq(value: R): Operator =
    Operator("${this.fullQualifiedName} != ?", value)

infix fun <T: Number, R: Number> Column<T>.lessEq(value: R): Operator =
    Operator("${this.fullQualifiedName} <= ?", value)

infix fun <T: Number, R: Number> Column<T>.less(value: R): Operator =
    Operator("${this.fullQualifiedName} < ?", value)

infix fun <T: Number, R: Number> Column<T>.grEq(value: R): Operator =
    Operator("${this.fullQualifiedName} >= ?", value)

infix fun <T: Number, R: Number> Column<T>.gr(value: R): Operator =
    Operator("${this.fullQualifiedName} > ?", value)

fun <T: Number, R: Number> Column<T>.inRange(l: R, r: R): Operator =
    Operator("${this.fullQualifiedName} >= ?", l) and Operator("${this.fullQualifiedName} <= ?", r)


@JvmName("eq")
infix fun Column<String>.eq(value: String): Operator =
    Operator("${this.fullQualifiedName} = ?", value)

@JvmName("eq_nullable")
infix fun Column<String?>.eq(value: String): Operator =
    Operator("${this.fullQualifiedName} = ?", value)

@JvmName("neq")
infix fun Column<String>.neq(value: String): Operator =
    Operator("${this.fullQualifiedName} != ?", value)

@JvmName("neq_nullable")
infix fun Column<String?>.neq(value: String): Operator =
    Operator("${this.fullQualifiedName} != ?", value)

@JvmName("like_nullable")
infix fun Column<String?>.like(value: String): Operator =
    Operator("${this.fullQualifiedName} LIKE ?", "%$value%")

@JvmName("like")
infix fun Column<String>.like(value: String): Operator =
    Operator("${this.fullQualifiedName} LIKE ?", "%$value%")

infix fun Column<Boolean>.eq(value: Boolean): Operator =
    Operator("${this.fullQualifiedName} = ?", value)

infix fun Column<Boolean>.neq(value: Boolean): Operator =
    Operator("${this.fullQualifiedName} != ?", value)

infix fun Column<*>.eq(value: Column<*>): Operator =
    Operator("${this.fullQualifiedName} = ${value.fullQualifiedName}")