package com.rmp.user.dto.trainings.intensity

import com.rmp.lib.utils.redis.SerializableClass
import kotlinx.serialization.Serializable

@Serializable
data class TrainingIntensityListOutputDto(
    val intensities: List<TrainingIntensityOutputDto>
): SerializableClass