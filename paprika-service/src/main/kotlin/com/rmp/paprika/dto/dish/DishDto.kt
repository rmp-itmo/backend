package com.rmp.paprika.dto.dish

import kotlinx.serialization.Serializable

@Serializable
data class DishDto (
    val id: Int,
    val name: String,
    val logo: String,
    val calories: Double,
    val protein: Double,
    val fat: Double,
    val carbohydrates: Double,
    val timeToCook: Int,
    val typeId: Int,
)