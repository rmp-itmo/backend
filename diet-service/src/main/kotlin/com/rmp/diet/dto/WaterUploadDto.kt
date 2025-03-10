package com.rmp.diet.dto

import com.rmp.lib.utils.redis.SerializableClass
import kotlinx.serialization.Serializable


@Serializable
data class WaterUploadDto(
    val volume: Float,
): SerializableClass