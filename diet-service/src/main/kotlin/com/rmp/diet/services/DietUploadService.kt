package com.rmp.diet.services

import com.rmp.diet.dto.DishUploadDto
import com.rmp.diet.dto.WaterUploadDto
import com.rmp.lib.shared.modules.diet.DietFoodLogModel
import com.rmp.lib.shared.modules.diet.DietWaterLogModel
import com.rmp.lib.shared.modules.dish.DishModel
import com.rmp.lib.utils.korm.insert
import com.rmp.lib.utils.korm.query.batch.newAutoCommitTransaction
import com.rmp.lib.utils.redis.RedisEvent
import com.rmp.lib.utils.redis.fsm.FsmService
import org.kodein.di.DI
import java.time.LocalDateTime
import java.time.ZoneOffset

class DietUploadService(di: DI): FsmService(di) {
    suspend fun uploadWater(redisEvent: RedisEvent) {
        val user = redisEvent.authorizedUser ?: throw Exception("Bad user")
        val data = redisEvent.parseData<WaterUploadDto>() ?: throw Exception("Bad data")
        if (data.volume < 0) throw Exception("Bad water volume provided")

        val transaction = newAutoCommitTransaction {
            this add DietWaterLogModel
                .insert {
                    it[userId] = user.id
                    it[createdAt] = LocalDateTime.now().toEpochSecond(ZoneOffset.UTC) // Взять Offset из конфига
                    it[volume] = data.volume
                }
        }
        redisEvent.switchOnDb(transaction)
    }
    suspend fun uploadIntake(redisEvent: RedisEvent) {
        val user = redisEvent.authorizedUser ?: throw Exception("Bad user")
        val data = redisEvent.parseData<DishUploadDto>() ?: throw Exception("Bad data")
        if (data.id == null && data.dish == null) throw Exception("Bad dish ID provided")
        if (data.id != null) {
            val transaction = newAutoCommitTransaction {
                this add DietFoodLogModel
                    .insert {
                        it[userId] = user.id
                        it[createdAt] = LocalDateTime.now().toEpochSecond(ZoneOffset.UTC)
                        it[dish] = data.id
                    }
            }
            redisEvent.switchOnDb(transaction)
        } else {
            val dish = data.dish!!
            val transaction = newAutoCommitTransaction {
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
                    }
            }
            redisEvent.switchOnDb(transaction)
        }
    }
}