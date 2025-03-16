package com.rmp.diet.dto.dish

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