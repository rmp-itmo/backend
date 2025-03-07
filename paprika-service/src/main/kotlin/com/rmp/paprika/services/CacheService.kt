package com.rmp.paprika.services

import com.rmp.lib.shared.modules.paprika.CacheModel
import com.rmp.lib.shared.modules.paprika.CacheToDishModel
import com.rmp.lib.utils.korm.Row
import com.rmp.lib.utils.korm.column.*
import com.rmp.lib.utils.korm.insert
import com.rmp.lib.utils.korm.query.builders.filter.Operator
import com.rmp.lib.utils.korm.query.builders.filter.and
import com.rmp.lib.utils.redis.fsm.FsmService
import com.rmp.paprika.dto.PaprikaInputDto
import com.rmp.paprika.dto.meal.MealOutputDto
import org.kodein.di.DI
import java.lang.Exception

class CacheService(di: DI) : FsmService(di) {
    private fun createMinMaxCond(min: Double, max: Double, field: Column<Double>): Operator =
        ((field lessEq max) and (field grEq min))

    private fun dishCountCond(dishCount: Int?): Operator {
        return if (dishCount != null && dishCount != 0)
            (CacheModel.dishCount eq dishCount)
        else
            (CacheModel.dishCount lessEq 10000)
    }

    private fun excludeDishesFromList(list: List<Int>): Operator {
        val ids = CacheToDishModel.select().where {
            (CacheToDishModel.dish inList list)
        }
        // It`ll be separated into 2 states of FSM: 1 - fetch ids, 2 - create Operator

        val idsList = mutableListOf<Int>()
            //.map { it[EatingCacheDishesModel.eatingCache].value }.distinct()

        if (idsList.isEmpty())
            return CacheModel.id notInList listOf(0)

        return CacheModel.id notInList idsList
    }

    fun saveEating(eatingOutputDto: MealOutputDto, paprikaInputDto: PaprikaInputDto, index: Int): Int {
        val eatingInput = paprikaInputDto.eatings[index]
        val micronutrients = eatingOutputDto.idealParams ?: throw Exception()
        val cache = CacheModel.insert {
            it[calories] = micronutrients.calories
            it[protein] = micronutrients.protein
            it[fat] = micronutrients.fat
            it[carbohydrates] = micronutrients.carbohydrates

            it[size] = eatingInput.size

            if (eatingInput.type != 0)
                it[type] = eatingInput.type
            it[dishCount] = eatingOutputDto.dishes.size

            it[useTimesFromCreation] = 0
            it[useTimesFromLastScrap] = 0
            it[onRemove] = false
        }

        // retrieve id on the next step
        val cacheId = 1

        val insert = CacheToDishModel.batchInsert(eatingOutputDto.dishes) {
            this[CacheToDishModel.dish] = it.id
            this[CacheToDishModel.mealCache] = cacheId
        }.named("Insert-Cache-Dishes")

        return cacheId
    }

    fun saveUserDiet(userId: Int, eatingName: String, cacheId: Int) {}

    fun findEating(paprikaInputDto: PaprikaInputDto, index: Int) {
        val eatingOptions = paprikaInputDto.eatings[index]
        val params = ParamsManager.process {
            withSize(eatingOptions.size)
            fromPaprikaInput(paprikaInputDto)
        }.params

        println(params)
        println(paprikaInputDto)
        println(eatingOptions)

        val cache = CacheModel.select().where {
            excludeDishesFromList(paprikaInputDto.excludeDishes).apply { println("Excluded $this") } and
            createMinMaxCond(params.minProtein, params.maxProtein, CacheModel.protein).apply { println("Protein $this") } and
            createMinMaxCond(params.minFat, params.maxFat, CacheModel.fat).apply { println("Fat $this") } and
            createMinMaxCond(
                params.minCarbohydrates,
                params.maxCarbohydrates,
                CacheModel.carbohydrates
            ).apply { println("Carbo $this") } and
            createMinMaxCond(params.calories * 0.99, params.calories * 1.01, CacheModel.calories).apply { println("Calories $this") } and
            dishCountCond(eatingOptions.dishCount).apply { println("Count $this") }
        }
//            .toList().apply { println(this@apply) }.filter {
//            it.dishes.all { dish -> !paprikaInputDto.excludeDishes.contains(dish.idValue) }
//        }
    }

    fun updateCacheUsage(rows: List<Row>) {
        val ids = rows.map { it[CacheModel.id] }

        CacheModel.update(CacheModel.id inList ids) {
            CacheModel.useTimesFromCreation += 1
            CacheModel.useTimesFromLastScrap += 1
        }
    }
}