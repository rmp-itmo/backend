package com.rmp.paprika.dto

import com.rmp.lib.utils.redis.SerializableClass
import com.rmp.paprika.dto.meal.MealOptionsDto
import com.rmp.paprika.dto.meal.ParametersDto
import kotlinx.serialization.Serializable

@Serializable
data class PaprikaInputDto (
    val calories: Double? = null,
    val params: ParametersDto? = null,
    val diet: Int = 1,
    val meals: List<MealOptionsDto>,
    var excludeDishes: List<Long> = listOf()
): SerializableClass