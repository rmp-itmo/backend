package com.rmp.diet.services

import com.rmp.diet.actions.target.check.DailyTargetCheckEventState
import com.rmp.diet.dto.target.check.TargetCheckResultDto
import com.rmp.diet.dto.target.check.TargetCheckSupportDto
import com.rmp.diet.dto.target.set.TargetSetInputDto
import com.rmp.diet.dto.target.set.TargetSetOutputDto
import com.rmp.lib.exceptions.BadRequestException
import com.rmp.lib.exceptions.ForbiddenException
import com.rmp.lib.exceptions.InternalServerException
import com.rmp.lib.shared.conf.AppConf
import com.rmp.lib.shared.dto.TimeDto
import com.rmp.lib.shared.modules.diet.DietDishLogModel
import com.rmp.lib.shared.modules.dish.UserMenuItem
import com.rmp.lib.shared.modules.dish.UserMenuModel
import com.rmp.lib.shared.modules.user.UserModel
import com.rmp.lib.utils.korm.column.eq
import com.rmp.lib.utils.korm.query.builders.filter.and
import com.rmp.lib.utils.korm.references.JoinType
import com.rmp.lib.utils.redis.RedisEvent
import com.rmp.lib.utils.redis.fsm.FsmService
import org.kodein.di.DI

class DailyTargetService(di: DI) : FsmService(di, AppConf.redis.diet) {
    suspend fun set(redisEvent: RedisEvent) {
        val user = redisEvent.authorizedUser ?: throw ForbiddenException()
        val data = redisEvent.parseData<TargetSetInputDto>() ?: throw BadRequestException("Bad target set data provided")

        newAutoCommitTransaction(redisEvent) {
            this add UserModel
                .update(UserModel.id eq user.id) {
                    if (data.water != null) {
                        UserModel.waterTarget.set(data.water)
                    }
                    if (data.calories != null) {
                        UserModel.caloriesTarget.set(data.calories)
                    }
                }
        }[UserModel]
        redisEvent.switchOnApi(TargetSetOutputDto("success"))
    }



    suspend fun check(redisEvent: RedisEvent) {
        val data = redisEvent.parseData<TimeDto>() ?: throw BadRequestException("Timestamp is not provided")
        val user = redisEvent.authorizedUser ?: throw ForbiddenException()

        val select = newTransaction(redisEvent) {
            this add UserModel
                .select(UserModel.waterTarget, UserModel.caloriesTarget)
                .where { UserModel.id eq user.id }
        }

        val targets = select[UserModel]?.firstOrNull() ?: throw InternalServerException("")

        val supportData = TargetCheckSupportDto(
            targets = Pair(targets[UserModel.caloriesTarget], targets[UserModel.waterTarget]),
            result = TargetCheckResultDto(),
            timestamp = data.date
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

    suspend fun checkDishes(redisEvent: RedisEvent) {
        val user = redisEvent.authorizedUser ?: throw ForbiddenException()
        val supportData = redisEvent.parseData<TargetCheckSupportDto>() ?: throw InternalServerException("No support data")
        val targets = supportData.targets ?: throw Exception("FSM state error")
        targets.first ?: throw Exception("Bad calories value provided")

        val userData = transaction(redisEvent) {
            this add UserModel
                .select(UserModel.caloriesCurrent)
                .where { UserModel.id eq user.id }

            this add UserMenuItem
                .select(UserMenuItem.dishId, UserMenuItem.mealId, UserMenuModel.name)
                .join(UserMenuModel, JoinType.LEFT, UserMenuModel.mealId eq UserMenuItem.mealId)
                .where { (UserMenuItem.userId eq user.id) and (UserMenuItem.checked eq true) }
        }

        val caloriesData = userData[UserModel]?.firstOrNull() ?: throw InternalServerException("Failed to fetch user")
        val menuData = userData[UserMenuItem] ?: listOf()

        val inserted = transaction(redisEvent) {
            this add DietDishLogModel.batchInsert(menuData) { it, _ ->
                this[DietDishLogModel.date] = supportData.timestamp
                this[DietDishLogModel.userId] = user.id
                this[DietDishLogModel.dish] = it[UserMenuItem.dishId]
                this[DietDishLogModel.mealName] = it[UserMenuModel.name]
                this[DietDishLogModel.mealId] = it[UserMenuItem.mealId]
            }.named("insert-log")
        }["insert-log"]?.size ?: 0

        if (menuData.size > inserted) throw InternalServerException("Insert failed")

        transaction(redisEvent) {
            this add UserMenuModel.delete(UserMenuModel.userId eq user.id)
            this add UserMenuItem.delete(UserMenuItem.userId eq user.id)
        }

        val calories = caloriesData[UserModel.caloriesCurrent]

        supportData.result.dishes = targets.first!! < calories

        redisEvent.switchOn(supportData, AppConf.redis.diet, redisEvent.mutate(DailyTargetCheckEventState.CHECK_WATER))
    }

    suspend fun checkWater(redisEvent: RedisEvent) {
        val data = redisEvent.parseData<TargetCheckSupportDto>() ?: throw InternalServerException("No support data")
        val user = redisEvent.authorizedUser ?: throw Exception("Bad user")
        val waterTarget = data.targets?.second

        if (waterTarget != null) {
            val userData = transaction(redisEvent) {
                this add UserModel
                    .select(UserModel.waterCurrent)
                    .where { UserModel.id eq user.id }
            }[UserModel]?.firstOrNull() ?: throw InternalServerException("Failed to fetch user")

            data.result.water = userData[UserModel.waterCurrent] > waterTarget
        }
        redisEvent.switchOn(data, AppConf.redis.diet, redisEvent.mutate(DailyTargetCheckEventState.UPDATE_STREAKS))
    }

    suspend fun updateStreaks(redisEvent: RedisEvent) {
        val data = redisEvent.parseData<TargetCheckSupportDto>() ?: throw InternalServerException("No support data")
        val user = redisEvent.authorizedUser ?: throw Exception("Bad user")


        autoCommitTransaction(redisEvent) {
            this add UserModel
                .update(UserModel.id eq user.id) {
                    if (data.result.water == true) {
                        UserModel.waterStreak += 1
                    } else {
                        this[UserModel.waterStreak] = 0
                    }

                    if (data.result.dishes == true) {
                        UserModel.caloriesStreak += 1
                    } else {
                        this[UserModel.caloriesStreak] = 0
                    }
                    this[UserModel.caloriesCurrent] = 0.0
                    this[UserModel.waterCurrent] = 0.0
                }
        }[UserModel]

        redisEvent.switchOnApi(data.result)
    }
}