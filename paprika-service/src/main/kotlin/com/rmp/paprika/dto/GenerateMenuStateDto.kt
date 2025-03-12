package com.rmp.paprika.dto

import com.rmp.lib.utils.redis.SerializableClass
import com.rmp.paprika.dto.meal.MealOptionsDto
import com.rmp.paprika.dto.meal.MealOutputDto
import kotlinx.serialization.Serializable

@Serializable
data class GenerateMenuStateDto(
    val paprikaInputDto: PaprikaInputDto,
    val generated: List<MealOutputDto> = listOf(),
    val index: Int = 0,
    val offset: Long = 1,
    val dishesCount: Long = 0L,
    val cacheId: Long? = null,
): SerializableClass {
    fun clearMealState(): GenerateMenuStateDto =
        GenerateMenuStateDto(paprikaInputDto, generated, index)

    fun nextMeal(): GenerateMenuStateDto =
        GenerateMenuStateDto(paprikaInputDto, generated,index + 1, offset, dishesCount)

    fun nextDishBatch(): GenerateMenuStateDto =
        GenerateMenuStateDto(paprikaInputDto, generated, index, offset + 1, dishesCount)

    fun appendMeal(meal: MealOutputDto): GenerateMenuStateDto =
        GenerateMenuStateDto(paprikaInputDto, generated + meal, index, offset, dishesCount)

    fun setDishCount(dishesCount: Long): GenerateMenuStateDto =
        GenerateMenuStateDto(paprikaInputDto, generated, index, offset, dishesCount)

    fun tempCacheId(cacheId: Long): GenerateMenuStateDto =
        GenerateMenuStateDto(paprikaInputDto, generated, index, offset, dishesCount, cacheId)

    val currentMealOptions: MealOptionsDto
        get() = paprikaInputDto.meals[index]
}
