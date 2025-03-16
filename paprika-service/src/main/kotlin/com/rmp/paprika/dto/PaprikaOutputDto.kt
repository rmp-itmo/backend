package com.rmp.paprika.dto

import com.rmp.lib.utils.redis.SerializableClass
import com.rmp.paprika.dto.dish.MacronutrientsDto
import com.rmp.paprika.dto.meal.MealOutputDto
import com.rmp.paprika.dto.mpsolver.ParametersDto
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
data class PaprikaOutputDto (
    @Transient val diet: Int = 1,
    val meals: List<MealOutputDto>,
    val params: MacronutrientsDto,
    val idealParams: ParametersDto
): SerializableClass