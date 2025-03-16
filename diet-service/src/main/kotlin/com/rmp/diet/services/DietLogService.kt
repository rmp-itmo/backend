package com.rmp.diet.services

import com.rmp.diet.actions.dish.log.DishLogEventState
import com.rmp.diet.dto.dish.CreateDishDto
import com.rmp.diet.dto.dish.log.DishLogUploadDto
import com.rmp.diet.dto.dish.log.DishLogOutputDto
import com.rmp.diet.dto.water.WaterLogOutputDto
import com.rmp.diet.dto.water.WaterLogUploadDto
import com.rmp.lib.exceptions.BadRequestException
import com.rmp.lib.exceptions.ForbiddenException
import com.rmp.lib.exceptions.InternalServerException
import com.rmp.lib.shared.conf.AppConf
import com.rmp.lib.shared.modules.diet.DietDishLogModel
import com.rmp.lib.shared.modules.diet.DietWaterLogModel
import com.rmp.lib.shared.modules.dish.DishModel
import com.rmp.lib.utils.korm.insert
import com.rmp.lib.utils.redis.RedisEvent
import com.rmp.lib.utils.redis.fsm.FsmService
import org.kodein.di.DI
import java.time.LocalDateTime
import java.time.ZoneOffset

class DietLogService(di: DI): FsmService(di, AppConf.redis.diet) {
    private val offset = AppConf.zoneOffset

    // Water Log //
    suspend fun uploadWater(redisEvent: RedisEvent) {
        val user = redisEvent.authorizedUser ?: throw Exception("Bad user")
        val data = redisEvent.parseData<WaterLogUploadDto>() ?: throw Exception("Bad data")
        if (data.volume < 0) throw Exception("Bad water volume provided")

        val inserted = newAutoCommitTransaction(redisEvent) {
            this add DietWaterLogModel
                .insert {
                    it[userId] = user.id
                    it[time] = LocalDateTime.now().toEpochSecond(ZoneOffset.ofHours(offset))
                    it[volume] = data.volume
                }.named("insert-water-log")
        }["insert-water-log"]?.firstOrNull() ?: throw InternalServerException("Insert failed")

        redisEvent.switchOnApi(WaterLogOutputDto(inserted[DietWaterLogModel.id]))
    }


    // Dish Log //
    suspend fun uploadDish(redisEvent: RedisEvent) {
        val user = redisEvent.authorizedUser ?: throw ForbiddenException()
        val data = redisEvent.parseData<DishLogUploadDto>() ?: throw BadRequestException("Bad data")
        if (data.id == null && data.dish == null) throw BadRequestException("Bad dish ID provided")

        // В Dto два поля: если id == null, значит пользователь указал свой рецепт и такого блюда у нас нет.

        if (data.id == null && data.dish != null) {
            redisEvent.switchOn(data.dish, AppConf.redis.diet, redisEvent.mutate(DishLogEventState.LOG_NEW))
            return
        }


        val inserted = newAutoCommitTransaction(redisEvent) {
            this add DietDishLogModel
                .insert {
                    it[userId] = user.id
                    it[time] = LocalDateTime.now().toEpochSecond(ZoneOffset.ofHours(offset))
                    it[dish] = data.id
                }.named("insert-dish-log")
        }["insert-dish-log"]?.firstOrNull() ?: throw InternalServerException("Insert failed")

        redisEvent.switchOnApi(DishLogOutputDto(inserted[DietDishLogModel.id]))
    }

    private suspend fun createDish(redisEvent: RedisEvent, dish: CreateDishDto, userId: Long): Long {
        val insert = newAutoCommitTransaction(redisEvent) {
            this add DishModel
                .insert {
                    it[name] = dish.name
                    it[description] = dish.description
                    it[portionsCount] = dish.portionsCount
                    it[imageUrl] = "" // Нет реализации загрузки фотографий
                    it[calories] = dish.calories
                    it[protein] = dish.protein
                    it[fat] = dish.fat
                    it[carbohydrates] = dish.carbohydrates
                    it[cookTime] = dish.timeToCook
                    it[author] = userId
                    it[type] = dish.typeId
                }.named("insert-dish")
        }["insert-dish"]?.firstOrNull() ?: throw InternalServerException("Insert failed")

        return insert[DishModel.id]
    }

    suspend fun logNew(redisEvent: RedisEvent) {
        val user = redisEvent.authorizedUser ?: throw ForbiddenException()
        val createDishDto = redisEvent.parseData<CreateDishDto>() ?: throw BadRequestException("Bad dish create dto provided")

        val dishId = createDish(redisEvent, createDishDto, user.id)

        val log = newAutoCommitTransaction(redisEvent) {
            this add DietDishLogModel
                .insert {
                    it[userId] = user.id
                    it[time] = LocalDateTime.now().toEpochSecond(ZoneOffset.ofHours(offset))
                    it[dish] = dishId
                }.named("insert-dish-log")
        }["insert-dish-log"]?.firstOrNull() ?: throw InternalServerException("Insert failed")

        redisEvent.switchOnApi(DishLogOutputDto(log[DietDishLogModel.id]))
    }
}