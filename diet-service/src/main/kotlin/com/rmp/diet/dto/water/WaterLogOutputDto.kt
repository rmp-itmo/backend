package com.rmp.diet.dto.water

import com.rmp.lib.utils.redis.SerializableClass
import kotlinx.serialization.Serializable

@Serializable
data class WaterLogOutputDto(
   val id: Long
): SerializableClass