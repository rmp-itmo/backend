package com.rmp.diet.dto.dish.service

import com.rmp.lib.utils.redis.SerializableClass
import kotlinx.serialization.Serializable

@Serializable
data class SIMPLEDishListOutput(
    val list: List<SIMPLEDishOutput>? = null,
):SerializableClass