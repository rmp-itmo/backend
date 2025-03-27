package com.rmp.diet.dto.menu

import com.rmp.diet.dto.dish.DishDto
import com.rmp.lib.utils.redis.SerializableClass
import kotlinx.serialization.Serializable

@Serializable
data class MenuHistoryOutputDto (
    val date: Int,
    val dishes: Map<String, List<DishDto>>
): SerializableClass