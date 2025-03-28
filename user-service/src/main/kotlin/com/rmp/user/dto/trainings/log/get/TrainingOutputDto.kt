package com.rmp.user.dto.trainings.log.get

import com.rmp.lib.utils.redis.SerializableClass
import kotlinx.serialization.Serializable

@Serializable
data class TrainingOutputDto(
    val id: Long,
    val start: String,
    val end: String,
    val calories: Double,
    val type: String,
    val intensity: String,
): SerializableClass