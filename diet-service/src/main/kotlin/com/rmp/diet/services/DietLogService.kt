package com.rmp.diet.services

import com.rmp.diet.actions.food.DishLogEventState
import com.rmp.diet.actions.water.WaterLogEventState
import com.rmp.diet.dto.dish.DishLogUploadDto
import com.rmp.diet.dto.dish.DishLogOutputDto
import com.rmp.diet.dto.water.WaterLogOutputDto
import com.rmp.diet.dto.water.WaterLogUploadDto
import com.rmp.lib.shared.conf.AppConf
import com.rmp.lib.shared.modules.diet.DietDishLogModel
import com.rmp.lib.shared.modules.diet.DietWaterLogModel
import com.rmp.lib.shared.modules.dish.DishModel
import com.rmp.lib.utils.korm.insert
import com.rmp.lib.utils.korm.query.batch.newAutoCommitTransaction
import com.rmp.lib.utils.redis.RedisEvent
import com.rmp.lib.utils.redis.fsm.FsmService
import org.kodein.di.DI
import java.time.LocalDateTime
import java.time.ZoneOffset

class DietLogService(di: DI): FsmService(di) {
    private val offset = AppConf.zonedOffset.offsetValue

    // Water Log //
    suspend fun uploadWater(redisEvent: RedisEvent) {
        val user = redisEvent.authorizedUser ?: throw Exception("Bad user")
        val data = redisEvent.parseData<WaterLogUploadDto>() ?: throw Exception("Bad data")
        if (data.volume < 0) throw Exception("Bad water volume provided")

        val transaction = newAutoCommitTransaction {
            this add DietWaterLogModel
                .insert {
                    it[userId] = user.id
                    it[createdAt] = LocalDateTime.now().toEpochSecond(ZoneOffset.ofHours(offset))
                    it[volume] = data.volume
                }.named("insert-water-log")
        }
        redisEvent.switchOnDb(transaction, redisEvent.mutateState(WaterLogEventState.UPLOADED, data))
    }

    suspend fun waterUploaded(redisEvent: RedisEvent) {
        val data = redisEvent.parseDb()["insert-water-log"]?.firstOrNull() ?: throw Exception("Bad data")

        redisEvent.switchOnApi(WaterLogOutputDto(data[DietWaterLogModel.id]))
    }

    // Dish Log //
    suspend fun uploadDish(redisEvent: RedisEvent) {
        val user = redisEvent.authorizedUser ?: throw Exception("Bad user")
        val data = redisEvent.parseData<DishLogUploadDto>() ?: throw Exception("Bad data")
        if (data.id == null && data.dish == null) throw Exception("Bad dish ID provided")

        // В Dto два пля: если id != null, значит пользователь указал свой рецепт и такого блюда у нас нет.

        if (data.id != null) {
            val transaction = newAutoCommitTransaction {
                this add DietDishLogModel
                    .insert {
                        it[userId] = user.id
                        it[createdAt] = LocalDateTime.now().toEpochSecond(ZoneOffset.ofHours(offset))
                        it[dish] = data.id
                    }.named("insert-dish-log")
            }
            redisEvent.switchOnDb(transaction, redisEvent.mutateState(DishLogEventState.CREATED))
        } else {
            val dish = data.dish!!
            val createTransaction = newAutoCommitTransaction {
                this add DishModel
                    .insert {
                        it[name] = dish.name
                        it[description] = dish.description
                        it[portionsCount] = dish.portionsCount
                        it[imageUrl] = "" // Нет реализации загрузки фотографий
                        it[calories] = dish.calories
                        it[fat] = dish.fat
                        it[carbohydrates] = dish.carbohydrates
                        it[cookTime] = dish.timeToCook
                        it[author] = user.id
                    }.named("insert-dish")
            }
            redisEvent.switchOnDb(createTransaction, redisEvent.mutateState(DishLogEventState.CREATE_DISH))
        }
    }

    suspend fun createDish(redisEvent: RedisEvent) {
        val user = redisEvent.authorizedUser ?: throw Exception("Bad user")
        val data = redisEvent.parseDb()["insert-dish"]?.firstOrNull() ?: throw Exception("Bad data")

        val transaction = newAutoCommitTransaction {
            this add DietDishLogModel
                .insert {
                    it[userId] = user.id
                    it[createdAt] = LocalDateTime.now().toEpochSecond(ZoneOffset.ofHours(offset))
                    it[dish] = data[DishModel.id]
                }.named("insert-dish-log")
        }
        redisEvent.switchOnDb(transaction, redisEvent.mutateState(DishLogEventState.CREATED))

    }

    suspend fun dishCreated(redisEvent: RedisEvent) {
        val data = redisEvent.parseDb()["insert-dish-log"]?.firstOrNull() ?: throw Exception("Bad data")

        redisEvent.switchOnApi(DishLogOutputDto(data[DietDishLogModel.id]))
    }
}