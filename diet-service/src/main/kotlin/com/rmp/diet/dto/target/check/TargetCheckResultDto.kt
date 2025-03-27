package com.rmp.diet.dto.target.check

import com.rmp.lib.utils.redis.SerializableClass
import kotlinx.serialization.Serializable

@Serializable
data class TargetCheckResultDto (
    var dishes: Boolean? = null,
    var water: Boolean? = null,
    var steps: Boolean? = null,
): SerializableClass