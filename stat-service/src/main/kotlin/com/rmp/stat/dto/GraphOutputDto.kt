package com.rmp.stat.dto

import com.rmp.lib.utils.redis.SerializableClass
import kotlinx.serialization.Serializable

@Serializable
data class GraphOutputDto (
    val avgValue: Double,
    val highestValue: Double,
    val lowestValue: Double,
    val points: Map<Int, Double>
): SerializableClass