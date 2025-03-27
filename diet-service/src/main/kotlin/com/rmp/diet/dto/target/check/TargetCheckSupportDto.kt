package com.rmp.diet.dto.target.check

import com.rmp.lib.utils.redis.SerializableClass
import kotlinx.serialization.Serializable

@Serializable
data class TargetCheckSupportDto(
    var targets: Pair<Double?, Double?>? = null,
    var dishesIds: List<Long>? = null,
    //YYYYMMDD
    val timestamp: Int,
    var result: TargetCheckResultDto
): SerializableClass