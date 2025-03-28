package com.rmp.user.dto.summary

import com.rmp.lib.utils.redis.SerializableClass
import kotlinx.serialization.Serializable

@Serializable
data class UserSummaryInputDto(
    // YYYYMMDD
    val date: Int
): SerializableClass