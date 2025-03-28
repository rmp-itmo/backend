package com.rmp.stat.dto

import com.rmp.lib.utils.redis.SerializableClass
import kotlinx.serialization.Serializable

@Serializable
data class GraphConfigurationDto (
    val day: String? = null,
    val month: String? = null,
    val year: Int,
): SerializableClass