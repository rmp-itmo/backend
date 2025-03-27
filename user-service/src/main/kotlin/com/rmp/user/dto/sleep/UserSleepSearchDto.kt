package com.rmp.user.dto.sleep

import com.rmp.lib.utils.redis.SerializableClass
import kotlinx.serialization.Serializable

@Serializable
data class UserSleepSearchDto (
    val dateFrom: Int,
    val dateTo: Int,
): SerializableClass