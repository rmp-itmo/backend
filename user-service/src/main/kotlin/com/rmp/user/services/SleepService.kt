package com.rmp.user.services

import com.rmp.lib.exceptions.BadRequestException
import com.rmp.lib.exceptions.ForbiddenException
import com.rmp.lib.exceptions.InternalServerException
import com.rmp.lib.shared.modules.sleep.SleepQualityModel
import com.rmp.lib.shared.modules.user.UserSleepModel
import com.rmp.lib.utils.korm.Row
import com.rmp.lib.utils.korm.column.eq
import com.rmp.lib.utils.korm.column.inRange
import com.rmp.lib.utils.korm.insert
import com.rmp.lib.utils.korm.query.builders.filter.and
import com.rmp.lib.utils.redis.RedisEvent
import com.rmp.lib.utils.redis.fsm.FsmService
import com.rmp.user.dto.sleep.UserSleepDto
import com.rmp.user.dto.sleep.UserSleepHistory
import com.rmp.user.dto.sleep.UserSleepInputDto
import com.rmp.user.dto.sleep.UserSleepSearchDto
import org.kodein.di.DI

class SleepService(di: DI): FsmService(di) {

    private fun Row.toDto(): UserSleepDto =
        UserSleepDto(
            this[UserSleepModel.id],
            this[UserSleepModel.userId],
            this[UserSleepModel.sleepHours],
            this[UserSleepModel.sleepMinutes],
            this[UserSleepModel.date],
            this[UserSleepModel.sleepQuality]
        )

    private fun List<Row>.toDto(): List<UserSleepDto> = map {
        it.toDto()
    }

    suspend fun setTodaySleep(redisEvent: RedisEvent) {
        val authorizedUser = redisEvent.authorizedUser ?: throw ForbiddenException()
        val sleepData = redisEvent.parseData<UserSleepInputDto>() ?: throw BadRequestException("Bad data provided")

        val insert = newTransaction(redisEvent) {
            this add UserSleepModel.insert {
                it[userId] = authorizedUser.id
                it[sleepHours] = sleepData.hours
                it[sleepMinutes] = sleepData.minutes
                it[sleepQuality] = sleepData.quality
                it[date] = sleepData.date
            }.named("insert-user-sleep")

            this add UserSleepModel.select().where {
                (UserSleepModel.userId eq authorizedUser.id) and
                        (UserSleepModel.date eq sleepData.date)
            }
        }

        val userSleepData = insert[UserSleepModel] ?: throw InternalServerException("Failed to insert sleep")

        val (oldest, currentSleepData) =  if (userSleepData.size > 1) {
            // If in DB more than one row on today, than we need to find oldest and remove it
            if (userSleepData[0][UserSleepModel.id] > userSleepData[1][UserSleepModel.id]) {
                userSleepData[0][UserSleepModel.id] to userSleepData[1]
            } else {
                userSleepData[1][UserSleepModel.id] to userSleepData[0]
            }
        } else {
            //If size <= 1 nothing to delete
            null to userSleepData.firstOrNull()
        }

        // if size < 1 -> throw exception due to failed insertion
        if (currentSleepData == null) throw InternalServerException("Failed to insert sleep")

        autoCommitTransaction(redisEvent) {
            // Remove oldest
            if (oldest != null)
                this add UserSleepModel.delete(UserSleepModel.id eq oldest)
        }

        redisEvent.switchOnApi(currentSleepData.toDto())
    }

    suspend fun getSleepHistory(redisEvent: RedisEvent) {
        val authorizedUser = redisEvent.authorizedUser ?: throw ForbiddenException()
        val userSleepSearchDto = redisEvent.parseData<UserSleepSearchDto>() ?: throw BadRequestException("Bad data provided")

        val userSleepData = newAutoCommitTransaction(redisEvent) {
            this add UserSleepModel.select().where {
                (UserSleepModel.userId eq authorizedUser.id) and
                UserSleepModel.date.inRange(userSleepSearchDto.dateFrom, userSleepSearchDto.dateTo)
            }
        }[UserSleepModel] ?: listOf()

        redisEvent.switchOnApi(UserSleepHistory(
            userSleepSearchDto.dateFrom,
            userSleepSearchDto.dateTo,
            userSleepData.toDto()
        ))
    }
}