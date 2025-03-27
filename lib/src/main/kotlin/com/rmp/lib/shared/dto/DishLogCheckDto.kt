package com.rmp.lib.shared.dto

import com.rmp.lib.utils.redis.SerializableClass
import kotlinx.serialization.Serializable

@Serializable
data class DishLogCheckDto(
    val menuItemId: Long,
    val check: Boolean,
    var calories: Double = 0.0
): SerializableClass