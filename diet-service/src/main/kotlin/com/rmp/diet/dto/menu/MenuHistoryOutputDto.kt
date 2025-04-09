package com.rmp.diet.dto.menu

import com.rmp.lib.shared.dto.DishDto
import com.rmp.lib.utils.redis.SerializableClass
import kotlinx.serialization.Serializable

@Serializable
data class MenuHistoryOutputDto (
    val caloriesTarget: Double,
    val date: Int,
    val dishes: Map<String, List<DishDto>>
): SerializableClass