package com.rmp.paprika.dto.meal

import kotlinx.serialization.Serializable

@Serializable
data class MealOptionsDto (
    val name: String,
    val size: Double,
    val type: Int = 1,
//    val time: Int,
    /*
        * super easy = 1
        * easy = 2
        * middle = 3
        * hard = 4
    */
    val difficulty: Int,
    val dishCount: Int? = null
)