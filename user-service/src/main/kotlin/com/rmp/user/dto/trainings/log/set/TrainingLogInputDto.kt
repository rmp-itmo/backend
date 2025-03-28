package com.rmp.user.dto.trainings.log.set

import com.rmp.lib.utils.redis.SerializableClass
import kotlinx.serialization.Serializable

@Serializable
data class TrainingLogInputDto(
    val type: Long,
    val intensity: Long,
    // HHMM
    val start: Int,
    val end: Int,
    // YYYYMMDD
    val date: Int
): SerializableClass