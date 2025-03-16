package com.rmp.auth.services

import com.rmp.auth.dto.TokenOutputDto
import com.rmp.lib.exceptions.ForbiddenException
import com.rmp.lib.shared.modules.user.UserLoginModel
import com.rmp.lib.utils.korm.column.eq
import com.rmp.lib.utils.korm.insert
import com.rmp.lib.utils.korm.query.builders.filter.and
import com.rmp.lib.utils.redis.RedisEvent
import com.rmp.lib.utils.redis.fsm.FsmService
import com.rmp.lib.utils.security.jwt.JwtUtil
import org.kodein.di.DI

class RefreshService(di: DI): FsmService(di) {
    suspend fun fetchUser(redisEvent: RedisEvent) {
        val authorizedUser = redisEvent.authorizedUser ?: throw ForbiddenException()

        val userData = newAutoCommitTransaction(redisEvent) {
            this add UserLoginModel
                .select(UserLoginModel.lastLogin, UserLoginModel.user)
                .where { (UserLoginModel.user eq authorizedUser.id) and (UserLoginModel.lastLogin eq authorizedUser.lastLogin!!) }
        }

        userData[UserLoginModel]?.firstOrNull() ?: throw ForbiddenException()

        updateLastLogin(redisEvent, authorizedUser.id)
    }

    suspend fun updateLastLogin(redisEvent: RedisEvent, userId: Long) {
        val loginAt = System.currentTimeMillis()

        val inserted = newAutoCommitTransaction(redisEvent) {
            this add UserLoginModel
                .delete(UserLoginModel.user eq userId)

            this add UserLoginModel
                .insert {
                    it[user] = userId
                    it[lastLogin] = loginAt
                }
                .named("insert")
        }

        inserted["insert"]?.firstOrNull() ?: throw ForbiddenException()

        val accessToken = JwtUtil.createToken(userId, null)
        val refreshToken = JwtUtil.createToken(userId, loginAt)

        redisEvent.switchOnApi(TokenOutputDto(accessToken, refreshToken))
    }
}