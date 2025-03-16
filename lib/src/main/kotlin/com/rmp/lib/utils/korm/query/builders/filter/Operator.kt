package com.rmp.lib.utils.korm.query.builders.filter

class Operator(
    val expression: String? = null,
    val param: Any? = null,
    val left: Operator? = null,
    val right: Operator? = null
) {
    private fun buildRecursive(): Pair<String, List<Any?>> {
        if (left == null && right == null) {
            val unwrappedParam = if (param == null || param !is List<*>) mutableListOf(param) else param

            return if (expression != null && param != null) Pair(expression, unwrappedParam)
            else if (expression != null) Pair(expression, emptyList())
            else Pair("", emptyList())
        }

        val (leftExp, leftParam) = left?.buildExpression() ?: return Pair("", mutableListOf())

        val (rightExp, rightParam) = right?.buildExpression() ?: return Pair("", mutableListOf())

        return if (leftExp == "")
            Pair("($rightExp)", rightParam)
        else if (rightExp == "")
            Pair("($leftExp)", rightParam)
        else
            Pair("($leftExp) $expression ($rightExp)", leftParam + rightParam)
    }

    fun buildExpression(): Pair<String, List<Any?>> =
        buildRecursive()

    override fun toString(): String {
        return "Operator $left $expression $right"
    }
}

infix fun Operator.and(other: Operator): Operator =
    Operator(
        expression = "and",
        left = this,
        right = other
    )

infix fun Operator.or(other: Operator): Operator =
    Operator(
        expression = "or",
        left = this,
        right = other
    )

@JvmName("operatorAndNullable")
infix fun Operator?.and(other: Operator): Operator =
    if (this == null) other
    else this and other

@JvmName("operatorOrNullable")
infix fun Operator?.or(other: Operator): Operator =
    if (this == null) other
    else this or other