package com.rmp.diet.dto.dish

import com.rmp.lib.utils.redis.SerializableClass
import kotlinx.serialization.Serializable

@Serializable
data class DishFilterDto (
    val typeId: Int,
    val nameFilter: String? = null
): SerializableClass