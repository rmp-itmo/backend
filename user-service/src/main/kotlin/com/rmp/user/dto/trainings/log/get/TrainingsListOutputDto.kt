package com.rmp.user.dto.trainings.log.get

import com.rmp.lib.utils.redis.SerializableClass
import kotlinx.serialization.Serializable

@Serializable
data class TrainingsListOutputDto(
    val trainings:  Map<Int, List<TrainingOutputDto>>
): SerializableClass