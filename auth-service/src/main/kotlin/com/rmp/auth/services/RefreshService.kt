package com.rmp.auth.services

import com.rmp.auth.actions.refresh.RefreshEventState
import com.rmp.auth.dto.TokenOutputDto
import com.rmp.lib.exceptions.ForbiddenException
import com.rmp.lib.shared.modules.auth.dto.AuthorizedUser
import com.rmp.lib.shared.modules.user.UserLoginModel
import com.rmp.lib.utils.korm.column.eq
import com.rmp.lib.utils.korm.insert
import com.rmp.lib.utils.korm.query.batch.newAutoCommitTransaction
import com.rmp.lib.utils.korm.query.builders.filter.and
import com.rmp.lib.utils.redis.RedisEvent
import com.rmp.lib.utils.redis.fsm.FsmService
import com.rmp.lib.utils.security.jwt.JwtUtil
import org.kodein.di.DI

class RefreshService(di: DI): FsmService(di) {
    suspend fun fetchUser(redisEvent: RedisEvent) {
        val authorizedUser = redisEvent.authorizedUser ?: throw ForbiddenException()

        val transaction = newAutoCommitTransaction {
            this add UserLoginModel
                .select(UserLoginModel.lastLogin, UserLoginModel.user)
                .where { (UserLoginModel.user eq authorizedUser.id) and (UserLoginModel.lastLogin eq authorizedUser.lastLogin!!) }
                .named("select-user")
        }

        redisEvent.switchOnDb(transaction, redisEvent.mutate(RefreshEventState.VERIFY, authorizedUser))
    }

    suspend fun updateLastLogin(redisEvent: RedisEvent, userId: Long) {
        val loginAt = System.currentTimeMillis()

        val transaction = newAutoCommitTransaction {
            this add UserLoginModel
                .delete(UserLoginModel.user eq userId)

            this add UserLoginModel
                .insert {
                    it[user] = userId
                    it[lastLogin] = loginAt
                }
                .named("insert")
        }

        redisEvent.switchOnDb(transaction, redisEvent.mutate(RefreshEventState.UPDATED, AuthorizedUser(userId, loginAt)))
    }

    suspend fun verify(redisEvent: RedisEvent) {
        redisEvent.parseDb()["select-user"]?.firstOrNull() ?: throw ForbiddenException()
        val authorizedUser = redisEvent.parseState<AuthorizedUser>() ?: throw ForbiddenException()

        updateLastLogin(redisEvent, authorizedUser.id)
    }

    suspend fun updated(redisEvent: RedisEvent) {
        redisEvent.parseDb()["insert"]?.firstOrNull() ?: throw ForbiddenException()
        val authorizedUser = redisEvent.parseState<AuthorizedUser>() ?: throw ForbiddenException()

        val accessToken = JwtUtil.createToken(authorizedUser.id, null)
        val refreshToken = JwtUtil.createToken(authorizedUser.id, authorizedUser.lastLogin ?: throw ForbiddenException())

        redisEvent.switchOnApi(TokenOutputDto(accessToken, refreshToken))
    }
}