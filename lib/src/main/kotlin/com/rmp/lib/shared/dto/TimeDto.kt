package com.rmp.lib.shared.dto

import com.rmp.lib.utils.redis.SerializableClass
import kotlinx.serialization.Serializable

@Serializable
data class TimeDto(
    // YYYYMMDD
    val date: Int
): SerializableClass