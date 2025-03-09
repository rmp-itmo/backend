package com.rmp.auth.services

import com.rmp.auth.actions.auth.AuthEventState
import com.rmp.auth.dto.AuthInputDto
import com.rmp.lib.exceptions.ForbiddenException
import com.rmp.lib.shared.modules.user.UserModel
import com.rmp.lib.utils.korm.column.eq
import com.rmp.lib.utils.korm.query.batch.newAutoCommitTransaction
import com.rmp.lib.utils.redis.RedisEvent
import com.rmp.lib.utils.redis.fsm.FsmService
import com.rmp.lib.utils.security.bcrypt.CryptoUtil
import org.kodein.di.DI
import org.kodein.di.instance

class AuthService(di: DI): FsmService(di) {
    private val refreshService: RefreshService by instance()

    suspend fun fetchUser(redisEvent: RedisEvent) {
        val data = redisEvent.parseData<AuthInputDto>() ?: throw ForbiddenException()

        val transaction = newAutoCommitTransaction {
            this add UserModel
                .select(UserModel.id, UserModel.login, UserModel.password)
                .where { UserModel.login eq data.login }
                .named("select-user")
        }

        redisEvent.switchOnDb(transaction, redisEvent.mutateState(AuthEventState.VERIFY, data))
    }

    suspend fun verify(redisEvent: RedisEvent) {
        val data = redisEvent.parseDb()["select-user"]?.firstOrNull() ?: throw ForbiddenException()
        val authDto = redisEvent.parseState<AuthInputDto>() ?: throw ForbiddenException()

        if (!CryptoUtil.compare(authDto.password, data[UserModel.password])) {
            throw ForbiddenException()
        }

        refreshService.updateLastLogin(redisEvent, data[UserModel.id])
    }
}