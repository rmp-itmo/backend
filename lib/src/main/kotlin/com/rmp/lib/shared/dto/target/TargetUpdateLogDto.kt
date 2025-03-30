package com.rmp.lib.shared.dto.target

import com.rmp.lib.utils.redis.SerializableClass
import kotlinx.serialization.Serializable

@Serializable
data class TargetUpdateLogDto(
    // YYYYMMDD
    val date: Int,
    val user: Long
): SerializableClass