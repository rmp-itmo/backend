package com.rmp.diet.dto.water.log

import com.rmp.lib.utils.redis.SerializableClass
import kotlinx.serialization.Serializable


@Serializable
data class WaterLogUploadDto(
    val volume: Double,
): SerializableClass