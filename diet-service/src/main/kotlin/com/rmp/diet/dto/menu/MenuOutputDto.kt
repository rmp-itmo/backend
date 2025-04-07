package com.rmp.diet.dto.menu

import com.rmp.lib.utils.redis.SerializableClass
import kotlinx.serialization.Serializable

@Serializable
data class MenuOutputDto (
    val meals: List<MealOutputDto>? = null,
    val targetCalories: Double,
    val params: MacronutrientsDto,
): SerializableClass