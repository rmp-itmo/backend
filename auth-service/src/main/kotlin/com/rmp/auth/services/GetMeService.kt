package com.rmp.auth.services

import com.rmp.auth.actions.getme.GetMeEventState
import com.rmp.auth.dto.UserOutputDto
import com.rmp.lib.exceptions.ForbiddenException
import com.rmp.lib.shared.modules.user.UserLoginModel
import com.rmp.lib.shared.modules.user.UserModel
import com.rmp.lib.utils.korm.column.eq
import com.rmp.lib.utils.korm.query.batch.newAutoCommitTransaction
import com.rmp.lib.utils.redis.RedisEvent
import com.rmp.lib.utils.redis.fsm.FsmService
import org.kodein.di.DI

class GetMeService(di: DI): FsmService(di) {
    suspend fun fetchUser(redisEvent: RedisEvent) {
        val data = redisEvent.authorizedUser ?: throw ForbiddenException()

        val transaction = newAutoCommitTransaction {
            this add UserModel
                .select(UserModel.id, UserModel.login, UserModel.name)
                .where { UserModel.id eq data.id }
                .named("select-user")

            this add UserLoginModel
                .select(UserLoginModel.lastLogin)
                .where { UserLoginModel.user eq data.id }
                .named("select-last-login")
        }

        redisEvent.switchOnDb(transaction, redisEvent.mutate(GetMeEventState.RESOLVED, data))
    }

    suspend fun resolved(redisEvent: RedisEvent) {
        val user = redisEvent.parseDb()["select-user"]?.firstOrNull() ?: throw ForbiddenException()
        val lastLogin = redisEvent.parseDb()["select-last-login"]?.firstOrNull() ?: throw ForbiddenException()

        redisEvent.switchOnApi(UserOutputDto(
            user[UserModel.id],
            user[UserModel.login],
            user[UserModel.name],
            lastLogin[UserLoginModel.lastLogin],
        ))
    }
}