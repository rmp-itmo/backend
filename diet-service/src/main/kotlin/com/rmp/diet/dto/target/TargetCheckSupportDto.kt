package com.rmp.diet.dto.target

import com.rmp.lib.utils.redis.SerializableClass
import kotlinx.serialization.Serializable

@Serializable
data class TargetCheckSupportDto(
    var targets: Pair<Double?, Double?>? = null,
    var dishesIds: List<Long>? = null,
    val timestamp: Long,
    var result: TargetCheckResultDto
): SerializableClass