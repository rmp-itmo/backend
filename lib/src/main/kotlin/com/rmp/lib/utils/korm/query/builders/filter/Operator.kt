package com.rmp.lib.utils.korm.query.builders.filter

class Operator(
    val expression: String? = null,
    val param: Any? = null,
    val left: Operator? = null,
    val right: Operator? = null
) {
    private fun buildRecursive(): Pair<String, List<Any?>> {
        if (left == null && right == null) {
            return Pair(expression!!, mutableListOf(param))
        }

        val (leftExp, leftParam) = left?.buildExpression() ?: return Pair("", mutableListOf())
        println(leftExp)
        println(leftParam)

        val (rightExp, rightParam) = right?.buildExpression() ?: return Pair("", mutableListOf())
        println(rightExp)
        println(rightParam)

        return Pair("($leftExp) $expression ($rightExp)", leftParam + rightParam)
    }

    fun buildExpression(): Pair<String, List<Any?>> =
        buildRecursive()
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