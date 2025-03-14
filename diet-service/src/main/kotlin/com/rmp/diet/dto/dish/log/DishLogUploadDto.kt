package com.rmp.diet.dto.dish.log

import com.rmp.diet.dto.dish.CreateDishDto
import com.rmp.lib.utils.redis.SerializableClass
import kotlinx.serialization.Serializable

@Serializable
data class DishLogUploadDto(
    val id: Long? = null,
    val dish: CreateDishDto? = null,
): SerializableClass