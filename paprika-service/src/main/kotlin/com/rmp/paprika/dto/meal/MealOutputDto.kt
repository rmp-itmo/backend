package com.rmp.paprika.dto.meal

import com.rmp.paprika.dto.dish.DishDto
import com.rmp.paprika.dto.dish.MacronutrientsDto
import kotlinx.serialization.Serializable

@Serializable
data class MealOutputDto (
    val name: String,
    var dishes: List<DishDto>,
    val params: MacronutrientsDto,
    val idealParams: MacronutrientsDto? = null,
    val cacheId: Long? = null
)
