package com.rmp.diet.services

import com.rmp.diet.actions.target.check.DailyTargetCheckEventState
import com.rmp.diet.dto.target.check.TargetCheckInputDto
import com.rmp.diet.dto.target.check.TargetCheckResultDto
import com.rmp.diet.dto.target.check.TargetCheckSupportDto
import com.rmp.lib.exceptions.BadRequestException
import com.rmp.lib.exceptions.ForbiddenException
import com.rmp.lib.exceptions.InternalServerException
import com.rmp.lib.shared.conf.AppConf
import com.rmp.lib.shared.modules.diet.DietDishLogModel
import com.rmp.lib.shared.modules.diet.DietWaterLogModel
import com.rmp.lib.shared.modules.dish.DishModel
import com.rmp.lib.shared.modules.user.UserModel
import com.rmp.lib.utils.korm.column.eq
import com.rmp.lib.utils.korm.column.grEq
import com.rmp.lib.utils.redis.RedisEvent
import com.rmp.lib.utils.redis.fsm.FsmService
import org.kodein.di.DI

class DietTargetCheckService(di: DI): FsmService(di) {

    suspend fun init(redisEvent: RedisEvent) {
        val data = redisEvent.parseData<TargetCheckInputDto>() ?: throw BadRequestException("Timestamp is not provided")
        val user = redisEvent.authorizedUser ?: throw ForbiddenException()
        val select = newAutoCommitTransaction(redisEvent) {
            this add UserModel
                .select(UserModel.waterTarget, UserModel.caloriesTarget)
                .where { UserModel.id eq user.id }
        }

        val targets = select[UserModel]?.firstOrNull() ?: throw InternalServerException("")

        val supportData = TargetCheckSupportDto(
            targets = Pair(targets[UserModel.caloriesTarget], targets[UserModel.waterTarget]),
            result = TargetCheckResultDto(),
            timestamp = data.timestamp
        )

        //If dish target exist
        if (supportData.targets?.first != null) {
            redisEvent.switchOn(
                supportData,
                AppConf.redis.diet,
                redisEvent.mutate(DailyTargetCheckEventState.CHECK_DISH))
            return
        }

        //If water target exist and dish target not
        if (supportData.targets?.second != null) {
            redisEvent.switchOn(
                supportData,
                AppConf.redis.diet,
                redisEvent.mutate(DailyTargetCheckEventState.CHECK_WATER))
        }
    }

    private suspend fun selectCalories(redisEvent: RedisEvent): Double {
        val supportData = redisEvent.parseData<TargetCheckSupportDto>() ?: throw InternalServerException("No support data")
        val user = redisEvent.authorizedUser ?: throw ForbiddenException()

        val dailyDishes = newAutoCommitTransaction(redisEvent) {
            this add DietDishLogModel
                .select(DishModel.calories)
                .join(DishModel)
                .where { DietDishLogModel.userId eq user.id }
                .andWhere { DietDishLogModel.time grEq  supportData.timestamp}
        }[DietDishLogModel] ?: listOf()

        return dailyDishes.sumOf { it[DishModel.calories] }
    }

    suspend fun checkDishes(redisEvent: RedisEvent) {
        val supportData = redisEvent.parseData<TargetCheckSupportDto>() ?: throw InternalServerException("No support data")
        val targets = supportData.targets ?: throw Exception("FSM state error")
        targets.first ?: throw Exception("Bad calories value provided")

        val calories = selectCalories(redisEvent.mutate(redisEvent.mutate(supportData)))

        supportData.result.dishes = targets.first!! < calories

        redisEvent.switchOn(supportData, AppConf.redis.diet, redisEvent.mutate(DailyTargetCheckEventState.CHECK_WATER))
    }

    suspend fun checkWater(redisEvent: RedisEvent) {
        val data = redisEvent.parseData<TargetCheckSupportDto>() ?: throw InternalServerException("No support data")
        val user = redisEvent.authorizedUser ?: throw Exception("Bad user")
        val waterTarget = data.targets?.second

        if (waterTarget != null) {
            val dailyWater = newAutoCommitTransaction(redisEvent) {
                this add DietWaterLogModel
                    .select(DietWaterLogModel.volume)
                    .where { DietWaterLogModel.userId eq user.id }
                    .andWhere { DietWaterLogModel.time grEq data.timestamp }
            }[DietWaterLogModel] ?: listOf()

            val sum = dailyWater.sumOf { it[DietWaterLogModel.volume] }

            data.result.water = waterTarget < sum
        }
        redisEvent.switchOn(data, AppConf.redis.diet, redisEvent.mutate(DailyTargetCheckEventState.UPDATE_STREAKS))
    }

    suspend fun updateStreaks(redisEvent: RedisEvent) {
        val data = redisEvent.parseData<TargetCheckSupportDto>() ?: throw InternalServerException("No support data")
        val user = redisEvent.authorizedUser ?: throw Exception("Bad user")

        val currentStreaks = newAutoCommitTransaction(redisEvent) {
            this add UserModel
                .select(UserModel.waterStreak, UserModel.caloriesStreak)
                .where { UserModel.id eq user.id }
        }[UserModel]?.firstOrNull() ?: throw InternalServerException("Streaks select error")

        newAutoCommitTransaction(redisEvent) {
            this add UserModel
                .update(UserModel.id eq user.id) {
                    if (data.result.water == true) {
                        UserModel.waterStreak.set(currentStreaks[UserModel.waterStreak] + 1)
                    } else {
                        UserModel.waterStreak.set(UserModel.waterStreak.defaultValue ?: 0)
                    }

                    if (data.result.dishes == true) {
                        UserModel.caloriesStreak.set(currentStreaks[UserModel.caloriesStreak] + 1)
                    } else {
                        UserModel.caloriesStreak.set(UserModel.caloriesStreak.defaultValue ?: 0)
                    }
                }.named("update-streaks")
        }["update-streaks"]

        redisEvent.switchOnApi(data.result)
    }
}