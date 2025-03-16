package com.rmp.diet.services

import com.rmp.diet.dto.target.set.TargetSetInputDto
import com.rmp.diet.dto.target.set.TargetSetOutputDto
import com.rmp.lib.exceptions.BadRequestException
import com.rmp.lib.exceptions.ForbiddenException
import com.rmp.lib.shared.modules.user.UserModel
import com.rmp.lib.utils.korm.column.eq
import com.rmp.lib.utils.redis.RedisEvent
import com.rmp.lib.utils.redis.fsm.FsmService
import org.kodein.di.DI

class DailyTargetSetService(di: DI): FsmService(di) {

    suspend fun init(redisEvent: RedisEvent) {
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
                }.named("update-targets")
        }
        redisEvent.switchOnApi(TargetSetOutputDto("success"))
    }
}