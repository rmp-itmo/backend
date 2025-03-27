package com.rmp.diet.dto.water.get

import com.rmp.lib.utils.redis.SerializableClass
import kotlinx.serialization.Serializable

@Serializable
data class WaterGetPerDayListOutputDto(
    val water: List<WaterGetPerDayOutputDto>?
): SerializableClass