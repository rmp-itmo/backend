package com.rmp.lib.shared.dto

import com.rmp.lib.utils.redis.SerializableClass
import kotlinx.serialization.Serializable

@Serializable
data class DishDto (
    val id: Long,
    val name: String,
    val description: String,
    val imageUrl: String,
    val portionsCount: Int,
    val calories: Double,
    val protein: Double,
    val fat: Double,
    val carbohydrates: Double,
    val timeToCook: Int,
    val typeId: Long,
    val menuItemId: Long? = null,
    val checked: Boolean? = null
): SerializableClass {
    operator fun plus(dish: DishDto): DishDto =
        DishDto(
            id,
            name,
            description,
            imageUrl,
            portionsCount,
            calories + dish.calories,
            protein + dish.protein,
            fat + dish.fat,
            carbohydrates + dish.carbohydrates,
            timeToCook,
            typeId,
        )
}