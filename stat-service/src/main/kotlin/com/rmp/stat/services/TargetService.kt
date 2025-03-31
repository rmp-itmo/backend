package com.rmp.stat.services

import com.rmp.lib.exceptions.InternalServerException
import com.rmp.lib.shared.conf.AppConf
import com.rmp.lib.shared.dto.target.TargetUpdateLogDto
import com.rmp.lib.shared.modules.target.TargetLogModel
import com.rmp.lib.shared.modules.user.UserModel
import com.rmp.lib.utils.korm.column.eq
import com.rmp.lib.utils.korm.insert
import com.rmp.lib.utils.korm.query.builders.filter.and
import com.rmp.lib.utils.redis.RedisEvent
import com.rmp.lib.utils.redis.fsm.FsmService
import org.kodein.di.DI

class TargetService(di: DI) : FsmService(di, AppConf.redis.stat) {
    suspend fun logTargets(redisEvent: RedisEvent) {
        val data = redisEvent.parseData<TargetUpdateLogDto>() ?: throw InternalServerException("Bad target set data provided")

        val select = newAutoCommitTransaction(redisEvent) {
            this add UserModel
                .select(UserModel.waterTarget, UserModel.caloriesTarget, UserModel.stepsTarget)
                .where { UserModel.id eq data.user }
        }[UserModel]?.firstOrNull() ?: throw InternalServerException("Select user error")

        val insert = newAutoCommitTransaction(redisEvent) {
            this add TargetLogModel
                .insert {
                    it[userId] = data.user
                    it[date] = data.date
                    it[waterTarget] = select[UserModel.waterTarget]
                    it[caloriesTarget] = select[UserModel.caloriesTarget]
                    it[stepTarget] = select[UserModel.stepsTarget]
                    it[sleepTarget] = AppConf.sleepTarget.toFloat()
                }.named("insert-target-update-log")

            this add TargetLogModel.select().where {
                (TargetLogModel.userId eq data.user) and
                        (TargetLogModel.date eq data.date)
            }
        }

        val targetLogData = insert[TargetLogModel] ?: throw InternalServerException("Insert failed")

        val (oldest, currentTargetData) =  if (targetLogData.size > 1) {
            // If in DB more than one row on today, than we need to find oldest and remove it
            if (targetLogData[0][TargetLogModel.id] > targetLogData[1][TargetLogModel.id]) {
                targetLogData[1][TargetLogModel.id] to targetLogData[0]
            } else {
                targetLogData[0][TargetLogModel.id] to targetLogData[1]
            }
        } else {
            //If size <= 1 nothing to delete
            null to targetLogData.firstOrNull()
        }

        // if size < 1 -> throw exception due to failed insertion
        if (currentTargetData == null) throw InternalServerException("Failed to insert sleep")

        newAutoCommitTransaction(redisEvent) {
            // Remove oldest
            if (oldest != null)
                this add TargetLogModel.delete(TargetLogModel.id eq oldest)
        }
    }
}