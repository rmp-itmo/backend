package com.rmp.diet.dto.menu

import kotlinx.serialization.Serializable

@Serializable
data class MacronutrientsDto (
    val calories: Double,
    val protein: Double,
    val fat: Double,
    val carbohydrates: Double,
) {
    operator fun plus(data: MacronutrientsDto): MacronutrientsDto =
        MacronutrientsDto(
            calories = calories + data.calories,
            protein = protein + data.protein,
            fat = fat + data.fat,
            carbohydrates = carbohydrates + data.carbohydrates
        )
}