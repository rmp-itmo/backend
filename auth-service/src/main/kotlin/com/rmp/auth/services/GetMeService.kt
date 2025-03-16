package com.rmp.auth.services

import com.rmp.auth.dto.UserOutputDto
import com.rmp.lib.exceptions.ForbiddenException
import com.rmp.lib.shared.modules.user.UserLoginModel
import com.rmp.lib.shared.modules.user.UserModel
import com.rmp.lib.utils.korm.column.eq
import com.rmp.lib.utils.redis.RedisEvent
import com.rmp.lib.utils.redis.fsm.FsmService
import org.kodein.di.DI

class GetMeService(di: DI): FsmService(di) {
    suspend fun fetchUser(redisEvent: RedisEvent) {
        val data = redisEvent.authorizedUser ?: throw ForbiddenException()

        val select = newAutoCommitTransaction(redisEvent) {
            this add UserModel
                .select(UserModel.id, UserModel.email, UserModel.name)
                .where { UserModel.id eq data.id }

            this add UserLoginModel
                .select(UserLoginModel.lastLogin)
                .where { UserLoginModel.user eq data.id }
        }

        val user = select[UserModel]?.firstOrNull() ?: throw ForbiddenException()
        val lastLogin = select[UserLoginModel]?.firstOrNull() ?: throw ForbiddenException()

        redisEvent.switchOnApi(UserOutputDto(
            user[UserModel.id],
            user[UserModel.email],
            user[UserModel.name],
            lastLogin[UserLoginModel.lastLogin],
        ))
    }
}