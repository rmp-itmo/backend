package com.rmp.diet.dto.menu

import com.rmp.lib.utils.redis.SerializableClass
import kotlinx.serialization.Serializable

@Serializable
data class MenuInputDto (
    val meals: List<MealInputDto>,
    val params: MacronutrientsDto,
): SerializableClass