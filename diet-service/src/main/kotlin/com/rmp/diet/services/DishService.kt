package com.rmp.diet.services

import com.rmp.diet.actions.dish.service.create.DishServiceCreateEventState
import com.rmp.diet.actions.dish.service.get.DishServiceGetAllEventState
import com.rmp.diet.dto.dish.log.DishLogOutputDto
import com.rmp.diet.dto.dish.service.SIMPLEDishCreate
import com.rmp.diet.dto.dish.service.SIMPLEDishListOutput
import com.rmp.diet.dto.dish.service.SIMPLEDishOutput
import com.rmp.lib.shared.modules.dish.DishModel
import com.rmp.lib.utils.korm.insert
import com.rmp.lib.utils.korm.query.BatchQuery
import com.rmp.lib.utils.korm.query.batch.newAutoCommitTransaction
import com.rmp.lib.utils.redis.RedisEvent
import com.rmp.lib.utils.redis.fsm.FsmService
import org.kodein.di.DI

class DishService(di: DI): FsmService(di) {
    suspend fun createDish(redisEvent: RedisEvent) {
        val data = redisEvent.parseData<SIMPLEDishCreate>() ?: throw Exception("Bad data")

        redisEvent.switchOnDb(
            createDishTransaction(data),
            redisEvent.mutate(DishServiceCreateEventState.CREATED)
        )
    }

    private fun createDishTransaction(dish: SIMPLEDishCreate): BatchQuery {
        return newAutoCommitTransaction {
            this add DishModel
                .insert {
                    it[name] = dish.name
                    it[description] = dish.description
                    it[portionsCount] = dish.portions
                    it[protein] = dish.protein
                    it[imageUrl] = dish.image
                    it[calories] = dish.calories
                    it[fat] = dish.fats
                    it[carbohydrates] = dish.carbo
                    it[cookTime] = dish.time
                    it[author] = 1
                    it[private] = false
                    it[type] = dish.type.toLong()
                }.named("service-create-dish")
        }
    }

    suspend fun dishCreated(redisEvent: RedisEvent) {
        val data = redisEvent.parseDb()["service-create-dish"]?.firstOrNull() ?: throw Exception("Bad data")
        redisEvent.switchOnApi(DishLogOutputDto(data[DishModel.id]))
    }

    suspend fun getDishes(redisEvent: RedisEvent) {
        val transaction = newAutoCommitTransaction {
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
                ).named("service-det-all-dishes")
        }
        redisEvent.switchOnDb(transaction, redisEvent.mutate(DishServiceGetAllEventState.RESPONSE))
    }
    suspend fun getResponse(redisEvent: RedisEvent){
        val data = redisEvent.parseDb()["service-det-all-dishes"] ?: throw Exception("Bad data")
        redisEvent.switchOnApi(
            SIMPLEDishListOutput(
                data.map {
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
}