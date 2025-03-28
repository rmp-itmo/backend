package com.rmp.user.dto.sleep

import com.rmp.lib.utils.redis.SerializableClass
import kotlinx.serialization.Serializable

@Serializable
data class UserSleepInputDto (
    val hours: Int,
    val minutes: Int,
    // YYYYMMDD format
    val date: Int,
    val quality: Long
): SerializableClass