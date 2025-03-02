package com.rmp.lib.utils.korm.query.builders

import com.rmp.lib.utils.korm.column.Column


open class FilterExpressionBuilder {
    val expressions: MutableList<String> = mutableListOf()
    val params: MutableList<Any> = mutableListOf()

    val empty: Boolean
        get() = expressions.isEmpty()

    infix fun Column<Int>.eq(value: Int) {
        expressions.add("${this.fullQualifiedName} = ?")
        params.add(value)
    }

    infix fun Column<Long>.eq(value: Long) {
        expressions.add("${this.fullQualifiedName} = ?")
        params.add(value)
    }

    @JvmName("eq")
    infix fun Column<String>.eq(value: String) {
        expressions.add("${this.fullQualifiedName} = ?")
        params.add(value)
    }

    @JvmName("eq_nullable")
    infix fun Column<String?>.eq(value: String) {
        expressions.add("${this.fullQualifiedName} = ?")
        params.add(value)
    }

    infix fun Column<*>.eq(value: Column<*>) {
        expressions.add("${this.fullQualifiedName} = ${value.fullQualifiedName}")
    }

    infix fun Column<Int>.less(value: Int) {
        expressions.add("${this.fullQualifiedName} < ?")
        params.add(value)
    }

    infix fun Column<Long>.less(value: Long) {
        expressions.add("${this.fullQualifiedName} < ?")
        params.add(value)
    }
}