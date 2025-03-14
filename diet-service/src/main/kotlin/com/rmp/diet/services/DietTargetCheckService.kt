package com.rmp.diet.services

import com.rmp.diet.actions.target.DailyTargetCheckEventState
import com.rmp.diet.dto.target.TargetCheckResultDto
import com.rmp.diet.dto.target.TargetCheckSupportDto
import com.rmp.lib.exceptions.InternalServerException
import com.rmp.lib.exceptions.UnauthorizedException
import com.rmp.lib.shared.conf.AppConf
import com.rmp.lib.shared.modules.diet.DietDishLogModel
import com.rmp.lib.shared.modules.diet.DietWaterLogModel
import com.rmp.lib.shared.modules.dish.DishModel
import com.rmp.lib.shared.modules.user.UserModel
import com.rmp.lib.utils.korm.column.eq
import com.rmp.lib.utils.korm.column.grEq
import com.rmp.lib.utils.korm.column.inList
import com.rmp.lib.utils.korm.query.batch.newAutoCommitTransaction
import com.rmp.lib.utils.redis.RedisEvent
import com.rmp.lib.utils.redis.fsm.FsmService
import org.kodein.di.DI
import java.time.LocalDate
import java.time.ZoneOffset

class DietTargetCheckService(di: DI): FsmService(di) {
    private val offset = AppConf.zoneOffset

    suspend fun selectTargets(redisEvent: RedisEvent) {
        val user = redisEvent.authorizedUser ?: throw UnauthorizedException()
        val transaction = newAutoCommitTransaction {
            this add UserModel
                .select(UserModel.waterTarget, UserModel.caloriesTarget)
                .where { UserModel.id eq user.id }
                .named("select-targets")
        }
        redisEvent.switchOnDb(transaction, redisEvent.mutate(DailyTargetCheckEventState.SELECTED_TARGETS))
    }

    suspend fun selectDailyDishes(redisEvent: RedisEvent) {
        val user = redisEvent.authorizedUser ?: throw UnauthorizedException()
        val targets = redisEvent.parseDb()["select-targets"]?.firstOrNull() ?: throw InternalServerException("")

        val supportData = TargetCheckSupportDto(
                targets = Pair(targets[UserModel.caloriesTarget], targets[UserModel.waterTarget]),
                result = TargetCheckResultDto()
            )
        targets[UserModel.caloriesTarget]?.let {
            val zoneOffset = ZoneOffset.ofHours(offset)
            val midnightTimestamp = LocalDate.now().atStartOfDay(zoneOffset).toEpochSecond()

            val transaction = newAutoCommitTransaction {
                this add DietDishLogModel
                    .select(DietDishLogModel.dish)
                    .where { DietDishLogModel.userId eq user.id }
                    .andWhere { DietDishLogModel.time grEq midnightTimestamp }
                    .named("select-daily-dishes")
            }

            // Такой странный подход передачи данных где-то в state, где-то просто через data обусловлен тем,
            // что switchOnDb не поддерживает передачи data, а имеет только state.

            redisEvent.switchOnDb(transaction, redisEvent.mutate(DailyTargetCheckEventState.SELECTED_DAILY_DISHES, supportData))
        } ?: redisEvent.switchOn(
            supportData.apply {
                supportData.result.dishes = false
            }
            , AppConf.redis.diet,
            redisEvent.mutate(
                DailyTargetCheckEventState.CHECKED_DISHES))
    }

    suspend fun selectCalories(redisEvent: RedisEvent) {
        val dishes = redisEvent.parseDb()["select-daily-dishes"] ?: throw Exception("Bad data")
        val supportData = redisEvent.parseState<TargetCheckSupportDto>() ?: throw Exception("Bad data")
        val dishesIds = dishes.map { it[DietDishLogModel.dish] }

        supportData.dishesIds = dishesIds
        val transaction = newAutoCommitTransaction {
            this add DishModel
                .select(DishModel.calories)
                .where { DishModel.id inList dishesIds}
                .named("select-calories")
        }
        redisEvent.switchOnDb(transaction, redisEvent.mutate(DailyTargetCheckEventState.SELECTED_CALORIES, supportData))
    }

    private fun sumCalories(dishes: List<Long>, calories: List<Double>): Double {
        if (calories.isEmpty()) {
            return 0.0
        }
        var index = 0
        var sum = calories.first()
        var previousDish = dishes.first()
        for (i in 1 until dishes.size) {
            if (previousDish != dishes[i]) {
                index++
            }
            sum += calories[index]
            previousDish = dishes[i]
        }
        return sum
    }

    suspend fun checkDishes(redisEvent: RedisEvent) {
        val calories = redisEvent.parseDb()["select-calories"] ?: throw Exception("Bad data")
        val supportData = redisEvent.parseState<TargetCheckSupportDto>() ?: throw Exception("No support data")
        val targets = supportData.targets ?: throw Exception("FSM state error")
        targets.first ?: throw Exception("Bad calories value provided")

        if (supportData.dishesIds == null) {
            redisEvent.switchOn(supportData, AppConf.redis.diet,
                redisEvent.mutate(
                    DailyTargetCheckEventState.CHECKED_DISHES))
        } else {
            val sum = sumCalories(supportData.dishesIds!!, calories.map { it[DishModel.calories] })

            supportData.result.dishes = targets.first!! < sum

            redisEvent.switchOn(
                supportData, AppConf.redis.diet,
                redisEvent.mutate(
                    DailyTargetCheckEventState.CHECKED_DISHES))
        }
    }

    suspend fun selectDailyWater(redisEvent: RedisEvent) {
        val data = redisEvent.parseData<TargetCheckSupportDto>() ?: throw Exception("Bad data")
        val user = redisEvent.authorizedUser ?: throw Exception("Bad user")

        data.targets?.second?.let {
            val zoneOffset = ZoneOffset.ofHours(offset)
            val midnightTimestamp = LocalDate.now().atStartOfDay(zoneOffset).toEpochSecond()

            val transaction = newAutoCommitTransaction {
                this add DietWaterLogModel
                    .select(DietWaterLogModel.volume)
                    .where { DietWaterLogModel.userId eq user.id }
                    .andWhere { DietWaterLogModel.time grEq midnightTimestamp }
                    .named("select-daily-water")
            }

            // Такой странный подход передачи данных где-то в state, где-то через data обусловлен тем,
            // что switchOnDb не поддерживает передачи data, а имеет только state.

            redisEvent.switchOnDb(transaction, redisEvent.mutate(DailyTargetCheckEventState.SELECTED_DAILY_WATER, data))
        } ?: redisEvent.switchOn(
            data.apply {
                data.result.water = false
            },
            AppConf.redis.diet,
            redisEvent.mutate(DailyTargetCheckEventState.CHECKED_WATER))
    }


    suspend fun checkWater(redisEvent: RedisEvent) {
        val data = redisEvent.parseState<TargetCheckSupportDto>() ?: throw Exception("Bad data")
        val water = redisEvent.parseDb()["select-daily-water"] ?: throw Exception("Bad data")
        val waterTarget = data.targets?.second?: throw Exception("Bad water target provided")

        val sum = water.sumOf { it[DietWaterLogModel.volume] }

        data.result.water = waterTarget < sum

        redisEvent.switchOn(data, AppConf.redis.diet, redisEvent.mutate(
            DailyTargetCheckEventState.CHECKED_WATER
        ))
    }

    suspend fun checked(redisEvent: RedisEvent) {
        val data = redisEvent.parseData<TargetCheckSupportDto>() ?: throw Exception("Bad data")

        redisEvent.switchOnApi(data.result)
    }
}