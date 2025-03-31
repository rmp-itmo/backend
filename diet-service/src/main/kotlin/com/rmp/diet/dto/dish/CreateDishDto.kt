package com.rmp.diet.dto.dish

import com.rmp.lib.utils.redis.SerializableClass
import kotlinx.serialization.Serializable

@Serializable
data class CreateDishDto (
    val name: String,
    val description: String,
    val image: String,
    val imageName: String,
    val portionsCount: Int,
    val calories: Double,
    val protein: Double,
    val fat: Double,
    val carbohydrates: Double,
    val timeToCook: Int,
    val typeId: Long,
): SerializableClass