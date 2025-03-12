package com.rmp.diet.dto.target

import com.rmp.lib.utils.redis.SerializableClass

data class TargetCheckResultDto (
    var dishes: Boolean? = null,
    var water: Boolean? = null,
): SerializableClass