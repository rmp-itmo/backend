package com.rmp.diet.dto.dish.log

import com.rmp.lib.utils.redis.SerializableClass
import kotlinx.serialization.Serializable

@Serializable
data class DishLogOutputDto(
    val id: Long,
): SerializableClass