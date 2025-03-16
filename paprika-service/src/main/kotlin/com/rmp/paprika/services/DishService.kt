package com.rmp.paprika.services

import com.rmp.lib.shared.modules.dish.DishModel
import com.rmp.lib.utils.korm.column.eq
import com.rmp.lib.utils.korm.column.lessEq
import com.rmp.lib.utils.korm.column.notInList
import com.rmp.lib.utils.korm.query.builders.SelectQueryBuilder
import com.rmp.lib.utils.korm.query.builders.filter.Operator
import com.rmp.lib.utils.korm.query.builders.filter.and
import com.rmp.lib.utils.redis.fsm.FsmService
import com.rmp.paprika.dto.PaprikaInputDto
import com.rmp.paprika.dto.meal.MealOptionsDto
import org.kodein.di.DI

class DishService(override val di: DI): FsmService(di) {
    private fun difficultyCond(difficulty: Int): Operator =
        when (difficulty) {
            1 -> DishModel.cookTime lessEq 5
            2 -> DishModel.cookTime lessEq 15
            3 -> DishModel.cookTime lessEq 30
            4 -> DishModel.cookTime lessEq 45
            5 -> DishModel.cookTime lessEq 60
            else -> DishModel.cookTime lessEq 1000
        }

    private fun createDishByParamsCond(mealOptionsDto: MealOptionsDto, paprikaInputDto: PaprikaInputDto): Operator =
        (DishModel.id notInList paprikaInputDto.excludeDishes) and
        (DishModel.type eq mealOptionsDto.type) and
        difficultyCond(mealOptionsDto.difficulty)

    fun getDishesIdByEatingParams(mealOptionsDto: MealOptionsDto, paprikaInputDto: PaprikaInputDto): SelectQueryBuilder<*> =
        DishModel.select().where { createDishByParamsCond(mealOptionsDto, paprikaInputDto) }

    fun getDishesByMealParams(mealOptionsDto: MealOptionsDto, paprikaInputDto: PaprikaInputDto, offset: Long = 1): SelectQueryBuilder<*> =
        DishModel.select().where {
            createDishByParamsCond(mealOptionsDto, paprikaInputDto)
        }.limit(750).offset(offset)
}