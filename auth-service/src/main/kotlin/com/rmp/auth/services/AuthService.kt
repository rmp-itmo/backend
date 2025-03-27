package com.rmp.auth.services

import com.rmp.auth.dto.AuthInputDto
import com.rmp.lib.exceptions.ForbiddenException
import com.rmp.lib.shared.modules.user.UserGoalTypeModel
import com.rmp.lib.shared.modules.user.UserModel
import com.rmp.lib.utils.korm.column.eq
import com.rmp.lib.utils.korm.column.neq
import com.rmp.lib.utils.redis.RedisEvent
import com.rmp.lib.utils.redis.fsm.FsmService
import com.rmp.lib.utils.security.bcrypt.CryptoUtil
import org.kodein.di.DI
import org.kodein.di.instance

class AuthService(di: DI): FsmService(di) {
    private val refreshService: RefreshService by instance()

    suspend fun fetchUser(redisEvent: RedisEvent) {
        val authInputDto = redisEvent.parseData<AuthInputDto>() ?: throw ForbiddenException()

        val response = newAutoCommitTransaction(redisEvent) {
            this add UserModel
                .select(UserModel.id, UserModel.email, UserModel.password)
                .where { UserModel.email eq authInputDto.login }

            this add UserGoalTypeModel.update(UserGoalTypeModel.name neq "Test") {
                this[UserGoalTypeModel.name] = "test"
            }

        }

        val data = response[UserModel]?.firstOrNull() ?: throw ForbiddenException()

        if (!CryptoUtil.compare(authInputDto.password, data[UserModel.password])) {
            throw ForbiddenException()
        }

        refreshService.updateLastLogin(redisEvent, data[UserModel.id])
    }
}