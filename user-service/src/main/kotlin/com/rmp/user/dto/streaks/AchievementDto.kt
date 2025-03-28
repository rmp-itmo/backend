package com.rmp.user.dto.streaks

import com.rmp.lib.utils.redis.SerializableClass
import kotlinx.serialization.Serializable

@Serializable
data class AchievementDto (
    val current: Int,
    val max: Int,
    val percentage: Int,
): SerializableClass