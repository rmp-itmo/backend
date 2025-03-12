package com.rmp.diet.services

import com.rmp.diet.actions.target.DailyTargetCheckEventState
import com.rmp.diet.dto.target.TargetCheckResultDto
import com.rmp.lib.shared.conf.AppConf
import com.rmp.lib.shared.modules.diet.DietDishLogModel
import com.rmp.lib.shared.modules.diet.DietWaterLogModel
import com.rmp.lib.shared.modules.dish.DishModel
import com.rmp.lib.shared.modules.user.UserModel
import com.rmp.lib.utils.korm.column.eq
import com.rmp.lib.utils.korm.column.grEq
import com.rmp.lib.utils.korm.column.inList
import com.rmp.lib.utils.korm.column.lessEq
import com.rmp.lib.utils.korm.query.batch.newAutoCommitTransaction
import com.rmp.lib.utils.redis.RedisEvent
import com.rmp.lib.utils.redis.fsm.FsmService
import org.kodein.di.DI
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset

class DietTargetCheckService(di: DI): FsmService(di) {
    private val offset = AppConf.zoneOffset

    suspend fun selectTargets(redisEvent: RedisEvent) {
        val user = redisEvent.authorizedUser ?: throw Exception("Bad user")
        val transaction = newAutoCommitTransaction {
            this add UserModel
                .select(UserModel.waterTarget, UserModel.caloriesTarget)
                .where { UserModel.id eq user.id }
                .named("select-targets")
        }
        redisEvent.switchOnDb(transaction, redisEvent.mutateState(DailyTargetCheckEventState.SELECTED_TARGETS))
    }

    suspend fun selectDailyDishes(redisEvent: RedisEvent) {
        val user = redisEvent.authorizedUser ?: throw Exception("Bad user")
        val targets = redisEvent.parseDb()["select-targets"]?.firstOrNull() ?: throw Exception("Bad data")

        targets[UserModel.caloriesTarget]?.let {
            val zoneOffset = ZoneOffset.ofHours(offset)
            val midnightTimestamp = LocalDate.now().atStartOfDay(zoneOffset).toEpochSecond()
            val currTimestamp = LocalDateTime.now().toEpochSecond(zoneOffset)

            val transaction = newAutoCommitTransaction {
                this add DietDishLogModel
                    .select(DietDishLogModel.dish)
                    .where { DietDishLogModel.userId eq user.id }
                    .andWhere { DietDishLogModel.createdAt lessEq currTimestamp }
                    .andWhere { DietDishLogModel.createdAt grEq midnightTimestamp }
                    .named("select-daily-dishes")
            }

            redisEvent.switchOnDb(transaction, redisEvent.mutateState(DailyTargetCheckEventState.SELECTED_DAILY_DISHES))
        } ?: redisEvent.switchOn<TargetCheckResultDto>(
            AppConf.redis.diet,
            redisEvent.mutateState(
                DailyTargetCheckEventState.CHECKED_DISHES,
                TargetCheckResultDto()))
    }

    suspend fun selectCalories(redisEvent: RedisEvent) {
        val dishes = redisEvent.parseDb()["select-daily-dishes"] ?: throw Exception("Bad data")

        val transaction = newAutoCommitTransaction {
            this add DishModel
                .select(DishModel.calories)
                .where { DishModel.id inList dishes.map { it[DishModel.id] } }
                .named("select-calories")
        }
        redisEvent.switchOnDb(transaction, redisEvent.mutateState(DailyTargetCheckEventState.SELECTED_CALORIES))
    }

    suspend fun checkDishes(redisEvent: RedisEvent) {
        val calories = redisEvent.parseDb()["select-calories"] ?: throw Exception("Bad data")
        val targets = redisEvent.parseDb()["select-targets"]?.firstOrNull() ?: throw Exception("Bad data")
        val caloriesTarget = targets[UserModel.caloriesTarget] ?: throw Exception("Bad data")

        val sum = calories.sumOf { it[DishModel.calories] }

        redisEvent.switchOn<TargetCheckResultDto>(
            AppConf.redis.diet, redisEvent.mutateState(
                DailyTargetCheckEventState.CHECKED_DISHES,
                TargetCheckResultDto(caloriesTarget < sum)
        ))
    }

    suspend fun selectDailyWater(redisEvent: RedisEvent) {
        val data = redisEvent.parseData<TargetCheckResultDto>() ?: throw Exception("Bad data")

        val user = redisEvent.authorizedUser ?: throw Exception("Bad user")
        val targets = redisEvent.parseDb()["select-targets"]?.firstOrNull() ?: throw Exception("Bad data")

        targets[UserModel.waterTarget]?.let {
            val zoneOffset = ZoneOffset.ofHours(offset)
            val midnightTimestamp = LocalDate.now().atStartOfDay(zoneOffset).toEpochSecond()
            val currTimestamp = LocalDateTime.now().toEpochSecond(zoneOffset)

            val transaction = newAutoCommitTransaction {
                this add DietWaterLogModel
                    .select(DietWaterLogModel.volume)
                    .where { DietWaterLogModel.userId eq user.id }
                    .andWhere { DietWaterLogModel.createdAt lessEq currTimestamp }
                    .andWhere { DietWaterLogModel.createdAt grEq midnightTimestamp }
                    .named("select-daily-water")
            }

            redisEvent.switchOnDb(transaction, redisEvent.mutateState(DailyTargetCheckEventState.SELECTED_DAILY_WATER, data))
        } ?: redisEvent.switchOn<TargetCheckResultDto>(
            AppConf.redis.diet,
            redisEvent.mutateState(DailyTargetCheckEventState.CHECKED_WATER, data))
    }


    suspend fun checkWater(redisEvent: RedisEvent) {
        val data = redisEvent.parseData<TargetCheckResultDto>() ?: throw Exception("Bad data")
        val water = redisEvent.parseDb()["select-daily-water"] ?: throw Exception("Bad data")
        val targets = redisEvent.parseDb()["select-targets"]?.firstOrNull() ?: throw Exception("Bad data")
        val waterTarget = targets[UserModel.waterTarget] ?: throw Exception("Bad data")

        val sum = water.sumOf { it[DietWaterLogModel.volume] }

        data.water = waterTarget < sum

        redisEvent.switchOn<TargetCheckResultDto>(AppConf.redis.diet, redisEvent.mutateState(
            DailyTargetCheckEventState.CHECKED_WATER,
            data
        ))
    }

    suspend fun checked(redisEvent: RedisEvent) {
        val data = redisEvent.parseData<TargetCheckResultDto>() ?: throw Exception("Bad data")

        redisEvent.switchOnApi(data)
    }
}