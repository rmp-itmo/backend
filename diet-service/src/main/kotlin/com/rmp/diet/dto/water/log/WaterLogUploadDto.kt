package com.rmp.diet.dto.water.log

import com.rmp.lib.utils.redis.SerializableClass
import kotlinx.serialization.Serializable


@Serializable
data class WaterLogUploadDto(
    // YYYYMMDD
    val date: Int,
    // HH:MM
    val time: String,
    val volume: Double,
): SerializableClass