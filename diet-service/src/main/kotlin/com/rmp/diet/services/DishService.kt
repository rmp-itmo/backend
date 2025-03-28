package com.rmp.diet.services

import com.rmp.diet.dto.dish.DishDto
import com.rmp.diet.dto.dish.DishFilterDto
import com.rmp.diet.dto.dish.DishListDto
import com.rmp.diet.dto.dish.service.SIMPLEDishCreate
import com.rmp.diet.dto.dish.service.SIMPLEDishListOutput
import com.rmp.diet.dto.dish.service.SIMPLEDishOutput
import com.rmp.lib.exceptions.BadRequestException
import com.rmp.lib.exceptions.InternalServerException
import com.rmp.lib.shared.dto.Response
import com.rmp.lib.shared.modules.dish.DishModel
import com.rmp.lib.utils.korm.Row
import com.rmp.lib.utils.korm.column.eq
import com.rmp.lib.utils.korm.column.like
import com.rmp.lib.utils.korm.insert
import com.rmp.lib.utils.redis.RedisEvent
import com.rmp.lib.utils.redis.fsm.FsmService
import org.kodein.di.DI

class DishService(di: DI): FsmService(di) {

    suspend fun createDish(redisEvent: RedisEvent) {
        val data = redisEvent.parseData<SIMPLEDishCreate>() ?: throw Exception("Bad data")

        val dish = newAutoCommitTransaction(redisEvent) {
            this add DishModel
                .insert {
                    it[name] = data.name
                    it[description] = data.description
                    it[portionsCount] = data.portions
                    it[protein] = data.protein
                    it[imageUrl] = data.image
                    it[calories] = data.calories
                    it[fat] = data.fats
                    it[carbohydrates] = data.carbo
                    it[cookTime] = data.time
                    it[author] = 1
                    it[private] = false
                    it[type] = data.type.toLong()
                }.named("create-dish")
        }["create-dish"]?.firstOrNull() ?: throw InternalServerException("Insert failed")

        redisEvent.switchOnApi(Response(true, dish[DishModel.id].toString()))
    }

    suspend fun getDishes(redisEvent: RedisEvent) {
        val dish = newAutoCommitTransaction(redisEvent) {
            this add DishModel
                .select(
                    DishModel.id,
                    DishModel.name,
                    DishModel.description,
                    DishModel.protein,
                    DishModel.fat,
                    DishModel.carbohydrates,
                    DishModel.calories,
                    DishModel.imageUrl,
                    DishModel.portionsCount,
                    DishModel.cookTime,
                    DishModel.type
                ).named("get-all-dishes")
        }["get-all-dishes"] ?: throw InternalServerException("Insert failed")

        redisEvent.switchOnApi(
            SIMPLEDishListOutput(
                dish.map {
                    SIMPLEDishOutput(
                        it[DishModel.id],
                        it[DishModel.protein],
                        it[DishModel.fat],
                        it[DishModel.carbohydrates],
                        it[DishModel.calories],
                        it[DishModel.imageUrl],
                        it[DishModel.description],
                        it[DishModel.name],
                        it[DishModel.portionsCount],
                        it[DishModel.cookTime],
                        it[DishModel.type]
                    )
                }
            )
        )
    }

    suspend fun getDishCalories(redisEvent: RedisEvent, dishId: Long): Double? =
        transaction(redisEvent) {
            this add DishModel.select(DishModel.calories).where { DishModel.id eq dishId }
        }[DishModel]?.firstOrNull()?.get(DishModel.calories)


    private fun List<Row>.toDto(): List<DishDto> = map {
        DishDto(
            it[DishModel.id],
            it[DishModel.name],
            it[DishModel.description],
            it[DishModel.imageUrl],
            it[DishModel.portionsCount],
            it[DishModel.calories],
            it[DishModel.protein],
            it[DishModel.fat],
            it[DishModel.carbohydrates],
            it[DishModel.cookTime],
            it[DishModel.type],
        )
    }

    suspend fun fetchDish(redisEvent: RedisEvent) {
        val filter = redisEvent.parseData<DishFilterDto>() ?: throw BadRequestException("Bad filter provided")

        val dishes = newAutoCommitTransaction(redisEvent) {
            this add DishModel
                        .select()
                        .where { (DishModel.type eq filter.typeId) }
                        .apply {
                            if (filter.nameFilter != null) {
                                andWhere { DishModel.name like filter.nameFilter }
                            }
                        }
        }[DishModel] ?: listOf()

        redisEvent.switchOnApi(DishListDto(filter, dishes.toDto()))
    }
}