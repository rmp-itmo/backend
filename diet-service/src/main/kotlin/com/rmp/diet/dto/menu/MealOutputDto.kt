package com.rmp.diet.dto.menu

import com.rmp.lib.shared.dto.DishDto
import com.rmp.lib.utils.redis.SerializableClass
import kotlinx.serialization.Serializable

@Serializable
data class MealOutputDto(
    val mealId: Long,
    val name: String,
    val dishes: List<DishDto>,
    val params: MacronutrientsDto,
    val typeId: Long = 0
): SerializableClass
