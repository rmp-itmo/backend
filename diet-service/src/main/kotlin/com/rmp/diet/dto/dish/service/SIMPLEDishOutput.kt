package com.rmp.diet.dto.dish.service

import com.rmp.lib.utils.redis.SerializableClass
import kotlinx.serialization.Serializable

@Serializable
data class SIMPLEDishOutput (
    val id: Long,
    val protein: Double,
    val fats: Double,
    val carbo: Double,
    val calories: Double,
    val image: String,
    val description: String,
    val name: String,
    val portions: Int,
    val time: Int,
    val type: Long,
): SerializableClass