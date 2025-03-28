package com.rmp.user.dto.trainings.types

import com.rmp.lib.utils.redis.SerializableClass
import kotlinx.serialization.Serializable

@Serializable
data class TrainingsTypeListOutputDto(
    val types: List<TrainingsTypeOutputDto>
): SerializableClass