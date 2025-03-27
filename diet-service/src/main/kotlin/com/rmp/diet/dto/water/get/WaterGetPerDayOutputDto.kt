package com.rmp.diet.dto.water.get

import com.rmp.lib.utils.redis.SerializableClass
import kotlinx.serialization.Serializable

@Serializable
data class WaterGetPerDayOutputDto(
    val time: Long,
    val volume: Double,
): SerializableClass