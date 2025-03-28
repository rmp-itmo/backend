package com.rmp.user.dto.trainings.intensity

import com.rmp.lib.utils.redis.SerializableClass
import kotlinx.serialization.Serializable

@Serializable
data class TrainingIntensityOutputDto(
    val id: Long,
    val name: String
): SerializableClass