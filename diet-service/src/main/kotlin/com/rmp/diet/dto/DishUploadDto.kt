package com.rmp.diet.dto

import com.rmp.lib.utils.redis.SerializableClass
import kotlinx.serialization.Serializable

@Serializable
data class DishUploadDto(
    val id: Long? = null,
    val dish: DishDto? = null,
): SerializableClass