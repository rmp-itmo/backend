package com.rmp.paprika.services

import com.rmp.lib.exceptions.BadRequestException
import com.rmp.lib.exceptions.InternalServerException
import com.rmp.lib.shared.modules.dish.DishModel
import com.rmp.lib.shared.modules.paprika.CacheModel
import com.rmp.lib.shared.modules.paprika.CacheToDishModel
import com.rmp.lib.utils.korm.column.*
import com.rmp.lib.utils.korm.query.batch.autoCommitTransaction
import com.rmp.lib.utils.korm.query.batch.newTransaction
import com.rmp.lib.utils.korm.query.builders.SelectQueryBuilder
import com.rmp.lib.utils.korm.query.builders.filter.Operator
import com.rmp.lib.utils.korm.query.builders.filter.and
import com.rmp.lib.utils.redis.RedisEvent
import com.rmp.lib.utils.redis.fsm.FsmService
import com.rmp.paprika.actions.cache.UpdateCacheState
import com.rmp.paprika.dto.GenerateMenuStateDto
import com.rmp.paprika.dto.PaprikaInputDto
import org.kodein.di.DI

class CacheService(di: DI) : FsmService(di) {
    private fun createMinMaxCond(min: Double, max: Double, field: Column<Double>): Operator =
        ((field lessEq max) and (field grEq min))

    private fun dishCountCond(dishCount: Int?): Operator {
        return if (dishCount != null && dishCount != 0)
            (CacheModel.dishCount eq dishCount)
        else
            (CacheModel.dishCount lessEq 10000)
    }

    fun findMeal(paprikaInputDto: PaprikaInputDto, index: Int): SelectQueryBuilder<*> {
        val mealOptions = paprikaInputDto.meals[index]
        val params = ParamsManager.process {
            withSize(mealOptions.size)
            fromPaprikaInput(paprikaInputDto)
        }.params

        return CacheModel.select().where {
            createMinMaxCond(params.minProtein, params.maxProtein, CacheModel.protein) and
            createMinMaxCond(params.minFat, params.maxFat, CacheModel.fat) and
            createMinMaxCond(
                params.minCarbohydrates,
                params.maxCarbohydrates,
                CacheModel.carbohydrates
            ) and
            createMinMaxCond(params.calories * 0.99, params.calories * 1.01, CacheModel.calories) and
            dishCountCond(mealOptions.dishCount)
        }
    }

    fun findIncompatible(excludedDishes: List<Long>): SelectQueryBuilder<*> =
        CacheToDishModel
            .select(CacheToDishModel.mealCache)
            .where {
                CacheToDishModel.dish inList excludedDishes
            }

    fun getMealDishes(cacheId: Long): SelectQueryBuilder<*> =
        CacheToDishModel
            .select()
            .join(DishModel)
            .where {
                CacheToDishModel.mealCache eq cacheId
            }

    suspend fun saveCache(redisEvent: RedisEvent) {
        val state = redisEvent.parseData<GenerateMenuStateDto>() ?: throw BadRequestException("")
        val ids = state.generated.mapNotNull { it.cacheId }

        var i = 0
        val newCache = state.generated.filter { it.cacheId == null }

        val transaction = newTransaction {
            if (ids.isNotEmpty())
                this add CacheModel.update(CacheModel.id inList ids) {
                    CacheModel.useTimesFromCreation += 1
                    CacheModel.useTimesFromLastScrap += 1
                }.named("update-exist")

            if (newCache.isNotEmpty())
                this add CacheModel.batchInsert(newCache) {
                        this[CacheModel.calories] = it.idealParams?.calories
                        this[CacheModel.protein] = it.idealParams?.protein
                        this[CacheModel.fat] = it.idealParams?.fat
                        this[CacheModel.carbohydrates] = it.idealParams?.carbohydrates
                        this[CacheModel.size] = state.paprikaInputDto.meals[i].size
                        this[CacheModel.type] = state.paprikaInputDto.meals[i].type
                        this[CacheModel.dishCount] = it.dishes.size

                        this[CacheModel.useTimesFromCreation] = 0
                        this[CacheModel.useTimesFromLastScrap] = 0
                        this[CacheModel.onRemove] = false

                        i += 1
                    }.named("insert-new")
        }

        redisEvent.switchOnDb(transaction, redisEvent.mutate(UpdateCacheState.SAVE_DISHES, state))
    }

    suspend fun saveDishes(redisEvent: RedisEvent) {
        val state = redisEvent.parseState<GenerateMenuStateDto>() ?: throw InternalServerException("Event state corrupted")
        val newCacheEntries = redisEvent.parseDb()["insert-new"] ?: listOf()

        val newCache = state.generated.filter { it.cacheId == null }

        val dishes: List<Pair<Long, Long>> =
            newCache.mapIndexed { index, it ->
                it.dishes.map { dish ->
                    newCacheEntries[index][CacheModel.id] to dish.id
                }
            }.flatten()

        val transaction = autoCommitTransaction {
            this add CacheToDishModel.batchInsert(dishes) { (cache, dish) ->
                this[CacheToDishModel.dish] = dish
                this[CacheToDishModel.mealCache] = cache
            }.named("Insert-Cache-Dishes")
        }

        redisEvent.switchOnDb(transaction, redisEvent.mutate(UpdateCacheState.SAVED))
    }
}