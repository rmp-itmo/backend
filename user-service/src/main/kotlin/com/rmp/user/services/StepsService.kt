package com.rmp.user.services

import com.rmp.lib.exceptions.BadRequestException
import com.rmp.lib.exceptions.ForbiddenException
import com.rmp.lib.exceptions.InternalServerException
import com.rmp.lib.shared.conf.AppConf
import com.rmp.lib.shared.dto.target.TargetCheckSupportDto
import com.rmp.lib.shared.dto.target.TargetUpdateLogDto
import com.rmp.lib.shared.modules.user.UserModel
import com.rmp.lib.shared.modules.user.UserStepsLogModel
import com.rmp.lib.utils.korm.column.eq
import com.rmp.lib.utils.korm.insert
import com.rmp.lib.utils.redis.RedisEvent
import com.rmp.lib.utils.redis.fsm.FsmService
import com.rmp.user.actions.steps.update.UserStepsUpdateEventState
import com.rmp.user.actions.target.UserTargetCheckEventState
import com.rmp.user.dto.UserCreateOutputDto
import com.rmp.user.dto.UserStepsUpdateDto
import com.rmp.user.dto.steps.StepsDto
import org.kodein.di.DI

class StepsService(di: DI): FsmService(di) {

    suspend fun logSteps(redisEvent: RedisEvent) {
        val user = redisEvent.authorizedUser ?: throw ForbiddenException()
        val stepsDto = redisEvent.parseData<StepsDto>() ?: throw BadRequestException("Invalid steps data provided")

        val currentSteps = newTransaction(redisEvent) {
            this add UserModel
                .select(UserModel.stepsCount)
                .where { UserModel.id eq user.id }
        }[UserModel]?.firstOrNull() ?: throw InternalServerException("Steps count is null")


        val newSteps =  currentSteps[UserModel.stepsCount] + stepsDto.count
        val update = newAutoCommitTransaction(redisEvent) {
            this add UserModel
                .update(UserModel.id eq user.id) {
                    UserModel.stepsCount.set(newSteps)
                }
        }

        update[UserModel]?.firstOrNull()?.get(UserModel.updateCount)
            ?: throw InternalServerException("Failed to update")

        redisEvent.switchOnApi(StepsDto(newSteps))
    }

    suspend fun updateStepsTarget(redisEvent: RedisEvent) {
        val user = redisEvent.authorizedUser?: throw ForbiddenException()
        val stepsDto = redisEvent.parseData<UserStepsUpdateDto>() ?: throw BadRequestException("Invalid steps value provided")

        val update = newAutoCommitTransaction(redisEvent) {
            this add UserModel
                .update(UserModel.id eq user.id) {
                    UserModel.stepsTarget.set(stepsDto.steps)
                }
        }

        val count = update[UserModel]?.firstOrNull()?.get(UserModel.updateCount)
        if (count == null || count < 1) throw InternalServerException("Failed to update")

        redisEvent
            .copyId("target-update-log")
            .switchOn(
                TargetUpdateLogDto(stepsDto.date, user.id),
                AppConf.redis.stat, redisEvent.mutate(UserStepsUpdateEventState.LOG))

        redisEvent.switchOnApi(UserCreateOutputDto(user.id))
    }

    suspend fun checkTarget(redisEvent: RedisEvent) {
        val authorizedUser = redisEvent.authorizedUser ?: throw ForbiddenException()
        val data = redisEvent.parseData<TargetCheckSupportDto>() ?: throw InternalServerException("Bad data provided")

        val select = newTransaction(redisEvent) {
            this add UserModel
                .select(UserModel.stepsCount)
                .where { UserModel.id eq authorizedUser.id }
        }[UserModel]?.firstOrNull() ?: throw InternalServerException("Failed to fetch steps count")

        val steps = select[UserModel.stepsCount]
        data.result.steps = data.targets.third < steps

        newAutoCommitTransaction(redisEvent) {
            this add UserStepsLogModel
                .insert {
                    it[user] = authorizedUser.id
                    it[count] = steps
                    it[date] = data.timestamp
                }.named("insert-steps-log")
        }["insert-steps-log"]?.firstOrNull() ?: throw InternalServerException("Insert failed")

        redisEvent.switchOn(data, AppConf.redis.diet, redisEvent.mutate(UserTargetCheckEventState.UPDATE_STREAKS))
    }
}