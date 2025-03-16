package com.rmp.diet.dto.menu

import com.rmp.lib.utils.redis.SerializableClass
import kotlinx.serialization.Serializable

@Serializable
data class MealInputDto (
    val name: String,
    var dishes: List<Long>,
): SerializableClass