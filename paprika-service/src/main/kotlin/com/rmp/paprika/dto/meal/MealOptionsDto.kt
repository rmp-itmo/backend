package com.rmp.paprika.dto.meal

import kotlinx.serialization.Serializable

@Serializable
data class MealOptionsDto (
    val name: String,
    val size: Double,
    val type: Long = 1,
//    val time: Int,
    /*
        * super easy = 1
        * easy = 2 + super easy
        * middle = 3 + easy + super easy
        * hard = 4 + easy + etc.
        * Any = 1000
    */
    val difficulty: Int = 1000,
    val dishCount: Int? = null
)