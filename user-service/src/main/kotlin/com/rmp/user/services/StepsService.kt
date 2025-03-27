package com.rmp.user.services

import com.rmp.lib.exceptions.BadRequestException
import com.rmp.lib.exceptions.ForbiddenException
import com.rmp.lib.exceptions.InternalServerException
import com.rmp.lib.shared.modules.user.UserModel
import com.rmp.lib.utils.korm.column.eq
import com.rmp.lib.utils.redis.RedisEvent
import com.rmp.lib.utils.redis.fsm.FsmService
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

        redisEvent.switchOnApi(UserCreateOutputDto(user.id))
    }
}