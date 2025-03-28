package com.rmp.diet.dto.water.get

import com.rmp.lib.utils.redis.SerializableClass
import kotlinx.serialization.Serializable

@Serializable
data class WaterHistoryItemOutputDto(
    val date: Int,
    val time: String,
    val volume: Double,
): SerializableClass