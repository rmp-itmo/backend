package com.rmp.lib.shared.dto

import com.rmp.lib.utils.redis.SerializableClass
import kotlinx.serialization.Serializable

@Serializable
data class CurrentCaloriesOutputDto (
    val dish: DishDto,
    val calories: Double
): SerializableClass