package com.rmp.diet.dto.target.set

import com.rmp.lib.utils.redis.SerializableClass
import kotlinx.serialization.Serializable

@Serializable
data class TargetSetInputDto (
    // YYYYMMDD
    val date: Int,
    val water: Double? = null,
    val calories: Double? = null,
): SerializableClass