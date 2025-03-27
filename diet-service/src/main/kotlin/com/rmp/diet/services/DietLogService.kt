package com.rmp.diet.services

import com.rmp.diet.actions.dish.log.DishLogEventState
import com.rmp.diet.dto.dish.DishDto
import com.rmp.diet.dto.menu.MenuHistoryOutputDto
import com.rmp.lib.shared.dto.DishLogCheckDto
import com.rmp.diet.dto.water.get.WaterHistoryOutputDto
import com.rmp.diet.dto.water.get.WaterHistoryItemOutputDto
import com.rmp.diet.dto.water.log.WaterLogOutputDto
import com.rmp.diet.dto.water.log.WaterLogUploadDto
import com.rmp.lib.exceptions.BadRequestException
import com.rmp.lib.exceptions.ForbiddenException
import com.rmp.lib.exceptions.InternalServerException
import com.rmp.lib.shared.conf.AppConf
import com.rmp.lib.shared.dto.TimeDto
import com.rmp.lib.shared.modules.diet.DietDishLogModel
import com.rmp.lib.shared.modules.diet.DietWaterLogModel
import com.rmp.lib.shared.modules.dish.DishModel
import com.rmp.lib.shared.modules.dish.UserMenuItem
import com.rmp.lib.shared.modules.dish.UserMenuItem.checked
import com.rmp.lib.shared.modules.user.UserModel
import com.rmp.lib.utils.korm.Row
import com.rmp.lib.utils.korm.column.eq
import com.rmp.lib.utils.korm.insert
import com.rmp.lib.utils.korm.query.builders.filter.and
import com.rmp.lib.utils.redis.RedisEvent
import com.rmp.lib.utils.redis.fsm.FsmService
import org.kodein.di.DI

class DietLogService(di: DI): FsmService(di, AppConf.redis.diet) {
    // Water Log //
    suspend fun uploadWater(redisEvent: RedisEvent) {
        val user = redisEvent.authorizedUser ?: throw Exception("Bad user")
        val data = redisEvent.parseData<WaterLogUploadDto>() ?: throw Exception("Bad data")
        if (data.volume < 0) throw Exception("Bad water volume provided")

        val inserted = newAutoCommitTransaction(redisEvent) {
            this add DietWaterLogModel
                .insert {
                    it[userId] = user.id
                    it[time] = data.time
                    it[date] = data.date
                    it[volume] = data.volume
                }.named("insert-water-log")

            this add UserModel.update(UserModel.id eq user.id) {
                UserModel.waterCurrent += data.volume
            }
        }["insert-water-log"]?.firstOrNull() ?: throw InternalServerException("Insert failed")

        redisEvent.switchOnApi(WaterLogOutputDto(inserted[DietWaterLogModel.id]))
    }

    // Dish Log //
    suspend fun uploadDish(redisEvent: RedisEvent) {
        val user = redisEvent.authorizedUser ?: throw ForbiddenException()
        val data = redisEvent.parseData<DishLogCheckDto>() ?: throw BadRequestException("Bad data")

        val menuItem = newTransaction(redisEvent) {
            this add UserMenuItem
                .select(checked, DishModel.calories)
                .join(DishModel)
                .where { (UserMenuItem.id eq data.menuItemId) and (UserMenuItem.userId eq user.id) }
        }[UserMenuItem]?.firstOrNull() ?: throw BadRequestException("Menu item not found")

        if (menuItem[checked] == data.check) {
            autoCommitTransaction(redisEvent) {}
            throw BadRequestException("Dish check is already = ${data.check}")
        }

        val dishData = transaction(redisEvent) {
            this add UserMenuItem.update((UserMenuItem.id eq data.menuItemId) and (UserMenuItem.userId eq user.id)) {
                this[checked] = data.check
            }
        }

        val updated = dishData[UserMenuItem]?.firstOrNull() ?: throw InternalServerException("Update failed")

        if (updated[UserMenuItem.updateCount] <= 0) throw BadRequestException("Bad id provided")
        data.calories = menuItem[DishModel.calories]

        redisEvent
            .copyId("update-calories")
            .switchOn(
                data,
                AppConf.redis.user,
                redisEvent.mutate(DishLogEventState.UPDATE_CALORIES)
            )
    }



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
            it[DishModel.type]
        )
    }

    suspend fun getWaterHistory(redisEvent: RedisEvent) {
        val user = redisEvent.authorizedUser ?: throw ForbiddenException()
        val day = redisEvent.parseData<TimeDto>() ?: throw BadRequestException("Bad data provided")

        val select = newAutoCommitTransaction(redisEvent) {
            this add DietWaterLogModel
                .select(DietWaterLogModel.date, DietWaterLogModel.time, DietWaterLogModel.volume)
                .where {
                    (DietWaterLogModel.userId eq user.id) and
                    (DietWaterLogModel.date eq day.date)
                }
        }[DietWaterLogModel]

        redisEvent.switchOnApi(
            WaterHistoryOutputDto(
                select?.map {
                    WaterHistoryItemOutputDto(
                        day.date,
                        it[DietWaterLogModel.time],
                        it[DietWaterLogModel.volume],
                    )
                }
            )
        )
    }

    suspend fun getMenuHistory(redisEvent: RedisEvent) {
        val user = redisEvent.authorizedUser ?: throw ForbiddenException()
        val day = redisEvent.parseData<TimeDto>() ?: throw BadRequestException("Bad data provided")

        val log = newAutoCommitTransaction(redisEvent) {
            this add DietDishLogModel
                .select()
                .join(DishModel)
                .where {
                    (DietDishLogModel.userId eq user.id) and (DietDishLogModel.date eq  day.date)
                }
        }[DietDishLogModel] ?: listOf()

        val dishesByMealName = log.groupBy { it[DietDishLogModel.mealName] }.mapValues { (_, v) -> v.toDto() }

        redisEvent.switchOnApi(
            MenuHistoryOutputDto(day.date, dishesByMealName)
        )
    }
}