package com.rmp.diet.dto.menu

import com.rmp.diet.dto.dish.CreateDishDto
import com.rmp.lib.utils.redis.SerializableClass
import kotlinx.serialization.Serializable

@Serializable
data class AddMenuItemDto (
    val mealId: Long,
    val dishId: Long? = null,
    val newDish: CreateDishDto? = null,
    val check: Boolean = false
): SerializableClass