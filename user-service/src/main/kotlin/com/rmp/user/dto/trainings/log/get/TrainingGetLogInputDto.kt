package com.rmp.user.dto.trainings.log.get

import com.rmp.lib.utils.redis.SerializableClass
import kotlinx.serialization.Serializable

@Serializable
data class TrainingGetLogInputDto(
    // YYYYMM
    val date: Int
): SerializableClass