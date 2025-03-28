package com.rmp.user.services

import com.rmp.lib.exceptions.BadRequestException
import com.rmp.lib.exceptions.ForbiddenException
import com.rmp.lib.exceptions.InternalServerException
import com.rmp.lib.shared.modules.sleep.SleepQualityModel
import com.rmp.lib.shared.conf.AppConf
import com.rmp.lib.shared.dto.target.TargetCheckSupportDto
import com.rmp.lib.shared.modules.sleep.SleepQuality
import com.rmp.lib.shared.modules.user.UserSleepModel
import com.rmp.lib.utils.korm.Row
import com.rmp.lib.utils.korm.column.eq
import com.rmp.lib.utils.korm.column.inRange
import com.rmp.lib.utils.korm.insert
import com.rmp.lib.utils.korm.query.builders.filter.and
import com.rmp.lib.utils.redis.RedisEvent
import com.rmp.lib.utils.redis.fsm.FsmService
import com.rmp.user.actions.summary.UserSummaryEventState
import com.rmp.user.actions.target.UserTargetCheckEventState
import com.rmp.user.dto.sleep.UserSleepDto
import com.rmp.user.dto.sleep.UserSleepHistory
import com.rmp.user.dto.sleep.UserSleepInputDto
import com.rmp.user.dto.sleep.UserSleepSearchDto
import com.rmp.user.dto.summary.UserSummaryInputDto
import org.kodein.di.DI

class SleepService(di: DI): FsmService(di) {
    private val sleepTarget = AppConf.sleepTarget

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

    suspend fun getSleepTimePerDay(redisEvent: RedisEvent) {
        val authorizedUser = redisEvent.authorizedUser ?: throw ForbiddenException()
        val data = redisEvent.parseData<UserSummaryInputDto>() ?: throw InternalServerException("Bad data provided")

        val sleep = newAutoCommitTransaction(redisEvent) {
            this add UserSleepModel
                .select(UserSleepModel.id, UserSleepModel.sleepHours, UserSleepModel.sleepMinutes)
                .where { (UserSleepModel.userId eq authorizedUser.id) and
                    (UserSleepModel.date eq data.date)
                }
        }[UserSleepModel]?.firstOrNull()

        redisEvent.switchOn(
            UserSleepDto(
                sleep?.get(UserSleepModel.id) ?: 0L,
                authorizedUser.id,
                sleep?.get(UserSleepModel.sleepHours) ?: 0,
                sleep?.get(UserSleepModel.sleepMinutes) ?: 0,
                data.date,
                SleepQuality.FINE.ordinal.toLong()
            ),
            AppConf.redis.user, redisEvent.mutate(UserSummaryEventState.SUMMARIZE))
    }

    suspend fun checkTarget(redisEvent: RedisEvent) {
        val authorizedUser = redisEvent.authorizedUser ?: throw ForbiddenException()
        val data = redisEvent.parseData<TargetCheckSupportDto>() ?: throw InternalServerException("Bad data provided")

        val sleep = newTransaction(redisEvent) {
            this add UserSleepModel
                .select(UserSleepModel.sleepHours)
                .where { (UserSleepModel.userId eq authorizedUser.id) and
                        (UserSleepModel.date eq data.timestamp)
                }
        }[UserSleepModel]?.firstOrNull()?.get(UserSleepModel.sleepHours) ?: 0

        data.result.sleep = sleepTarget <= sleep

        redisEvent.switchOn(data, AppConf.redis.user, redisEvent.mutate(UserTargetCheckEventState.CHECK_STEPS))
    }
}