package com.rmp.user.services

import com.rmp.lib.exceptions.BadRequestException
import com.rmp.lib.exceptions.ForbiddenException
import com.rmp.lib.exceptions.InternalServerException
import com.rmp.lib.shared.modules.user.UserHeartLogModel
import com.rmp.lib.utils.korm.insert
import com.rmp.lib.utils.redis.RedisEvent
import com.rmp.lib.utils.redis.fsm.FsmService
import com.rmp.user.dto.heart.HeartLogInputDto
import com.rmp.user.dto.heart.HeartLogOutputDto
import org.kodein.di.DI

class HeartService(di: DI): FsmService(di) {
    suspend fun heartLog(redisEvent: RedisEvent) {
        val authorizedUser = redisEvent.authorizedUser ?: throw ForbiddenException()
        val data = redisEvent.parseData<HeartLogInputDto>() ?: throw BadRequestException("Bad heart data provided")

        val insert = newAutoCommitTransaction(redisEvent) {
            this add UserHeartLogModel
                .insert {
                    it[heartRate] = data.heartRate
                    it[date] = data.date
                    it[time] = data.time
                    it[user] = authorizedUser.id
                }.named("insert-heart-log")
        }["insert-heart-log"]?.firstOrNull() ?: throw InternalServerException("Insert heart log failed")

        redisEvent.switchOnApi(HeartLogOutputDto(insert[UserHeartLogModel.id]))
    }
}