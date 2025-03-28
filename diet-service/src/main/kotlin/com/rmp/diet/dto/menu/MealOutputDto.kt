package com.rmp.diet.dto.menu

import com.rmp.diet.dto.dish.DishDto
import com.rmp.lib.utils.redis.SerializableClass
import kotlinx.serialization.Serializable

@Serializable
data class MealOutputDto(
    val mealId: Long,
    val name: String,
    val dishes: List<DishDto>,
    val params: MacronutrientsDto
): SerializableClass
