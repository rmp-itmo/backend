package com.rmp.user.dto.trainings.log.set

import com.rmp.lib.utils.redis.SerializableClass
import kotlinx.serialization.Serializable

@Serializable
data class TrainingLogOutputDto(
    val id: Long
): SerializableClass