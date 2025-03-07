package com.rmp.paprika.dto.dish

import kotlinx.serialization.Serializable

@Serializable
data class MacronutrientsDto (
    val calories: Double,
    val protein: Double,
    val fat: Double,
    val carbohydrates: Double,
)