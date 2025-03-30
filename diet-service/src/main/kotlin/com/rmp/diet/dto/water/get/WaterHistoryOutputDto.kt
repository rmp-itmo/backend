package com.rmp.diet.dto.water.get

import com.rmp.lib.utils.redis.SerializableClass
import kotlinx.serialization.Serializable

@Serializable
data class WaterHistoryOutputDto(
    val waterTarget: Double? = null,
    val water: List<WaterHistoryItemOutputDto>?
): SerializableClass