package com.rmp.diet.dto.dish

import com.rmp.lib.utils.redis.SerializableClass
import kotlinx.serialization.Serializable

@Serializable
data class DishListDto (
    val filter: DishFilterDto,
    val dishes: List<DishDto>
): SerializableClass