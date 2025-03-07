package com.rmp.paprika.services

import com.rmp.lib.shared.modules.dish.DishModel
import com.rmp.lib.utils.korm.column.eq
import com.rmp.lib.utils.korm.column.lessEq
import com.rmp.lib.utils.korm.column.neq
import com.rmp.lib.utils.korm.column.notInList
import com.rmp.lib.utils.korm.query.QueryDto
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

    private fun typeCond(type: Int): Operator {
        return if (type == 0)
            DishModel.type neq 0
        else
            DishModel.type eq type
    }

    private fun createDishByParamsCond(mealOptionsDto: MealOptionsDto, paprikaInputDto: PaprikaInputDto): Operator =
        DishModel.id notInList paprikaInputDto.excludeDishes and
//        dietCond(paprikaInputDto.diet) and
        difficultyCond(mealOptionsDto.difficulty)
//        typeCond(eatingOptionsDto.type)

    fun getDishesIdByEatingParams(mealOptionsDto: MealOptionsDto, paprikaInputDto: PaprikaInputDto) =
        DishModel.select(DishModel.id).where { createDishByParamsCond(mealOptionsDto, paprikaInputDto) }

    fun getDishesByEatingParams(mealOptionsDto: MealOptionsDto, paprikaInputDto: PaprikaInputDto, offset: Long = 1): Pair<String, QueryDto> =
        DishModel.select().where {
            createDishByParamsCond(mealOptionsDto, paprikaInputDto)
        }.limit(750).offset(offset).named("get-dishes")

//    fun removeSimple(id: Int?): Any = transaction {
//        DishModel.deleteWhere {
//            DishModel.id eq id
//        }
//        ""
//    }
//
//    fun createSimple(dish: SIMPLEDishCreate): Any = transaction {
//        DishModel.insert {
//            it[name] = dish.name
//            it[description] = dish.description
//            it[portionsCount] = dish.portions
//            it[imageUrl] = dish.image
//            it[calories] = dish.calories
//            it[protein] = dish.protein
//            it[fat] = dish.fats
//            it[carbohydrates] = dish.carbo
//            it[cellulose] = 0.0
//            it[timeToCook] = dish.time
//            it[diet] = dish.diet
//            it[type] = dish.type
//        }
//        ""
//    }
//
//    fun getDishesSimple(): Any = transaction {
//        DishModel
//            .selectAll()
//            .map {
//                SIMPLEDishOutput(
//                    it[DishModel.id].value,
//                    it[DishModel.protein],
//                    it[DishModel.fat],
//                    it[DishModel.carbohydrates],
//                    it[DishModel.calories],
//                    it[DishModel.imageUrl],
//                    it[DishModel.description],
//                    it[DishModel.name],
//                    it[DishModel.portionsCount],
//                    it[DishModel.timeToCook],
//                    it[DishModel.diet].value,
//                    it[DishModel.type].value,
//                )
//            }
//    }


}