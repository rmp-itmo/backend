package com.rmp.paprika.dto

import com.rmp.paprika.dto.meal.MealOptionsDto
import com.rmp.paprika.dto.meal.ParametersDto
import kotlinx.serialization.Serializable

@Serializable
data class PaprikaInputDto (
    val calories: Double? = null,
    val params: ParametersDto? = null,
    val diet: Int = 1,
    val eatings: List<MealOptionsDto>,
    var excludeDishes: List<Int> = listOf()
)