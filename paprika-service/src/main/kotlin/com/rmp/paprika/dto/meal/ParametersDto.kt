package com.rmp.paprika.dto.meal

import com.rmp.lib.shared.modules.paprika.CacheModel
import com.rmp.lib.utils.korm.Row
import com.rmp.paprika.dto.dish.MacronutrientsDto
import kotlinx.serialization.Serializable

@Serializable
data class ParametersDto (
    val calories: Double,

    val minProtein: Double,
    val maxProtein: Double,

    val minFat: Double,
    val maxFat: Double,

    val minCarbohydrates: Double,
    val maxCarbohydrates: Double,

) {
    companion object {
        fun buildFromCache(data: Row): ParametersDto = ParametersDto(
            calories = data[CacheModel.calories],
            minProtein = data[CacheModel.protein],
            maxProtein = data[CacheModel.protein],
            minFat = data[CacheModel.fat],
            maxFat = data[CacheModel.fat],
            minCarbohydrates = data[CacheModel.carbohydrates],
            maxCarbohydrates = data[CacheModel.carbohydrates]
        )

        fun buildFromMacronutrients(data: MacronutrientsDto) = ParametersDto(
            calories = data.calories,
            minProtein = data.protein,
            maxProtein = data.protein,
            minFat = data.fat,
            maxFat = data.fat,
            minCarbohydrates = data.carbohydrates,
            maxCarbohydrates = data.carbohydrates
        )
    }
}