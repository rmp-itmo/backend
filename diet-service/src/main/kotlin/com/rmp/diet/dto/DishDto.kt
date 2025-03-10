package com.rmp.diet.dto

import kotlinx.serialization.Serializable

@Serializable
data class DishDto (
    val name: String,
    val description: String,
    val portionsCount: Int,
    val calories: Double,
    val protein: Double,
    val fat: Double,
    val carbohydrates: Double,
    val timeToCook: Int,
    val typeId: Int,
)