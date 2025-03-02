package com.rmp.auth.services

import com.rmp.auth.actions.AuthEventState
import com.rmp.auth.dto.AuthInputDto
import com.rmp.auth.dto.TokenOutputDto
import com.rmp.lib.exceptions.ForbiddenException
import com.rmp.lib.shared.modules.user.UserModel
import com.rmp.lib.utils.korm.query.batch.newAutoCommitTransaction
import com.rmp.lib.utils.korm.query.batch.newTransaction
import com.rmp.lib.utils.redis.RedisEvent
import com.rmp.lib.utils.redis.fsm.FsmService
import org.kodein.di.DI

class AuthService(di: DI): FsmService(di) {
    suspend fun fetchUser(redisEvent: RedisEvent) {
        val data = redisEvent.parseData<AuthInputDto>() ?: throw Exception("Bad data")
        /*
        val newTransaction = newTransaction {
            // Transaction initialized
            this add UserModel.select(UserModel.login, UserModel.password).where {
                    UserModel.login eq data.login
                }.named("select")

            // And wait for commit
        }

        val newAutoCommit = newAutoCommitTransaction {
            // Transaction initialized
            this add UserModel.select(UserModel.login, UserModel.password).where {
                    UserModel.login eq data.login
                }.named("select")
            // And automatically commits after all queries in batch are executed
        }

        val batchQuery = buildBatch {
            this add UserModel.select(UserModel.login, UserModel.password).where {
                        UserModel.login eq data.login
                    }.named("select")

            this add UserModel.delete {
                        UserModel.password eq data.password
                    }

            rollback()

            this add UserModel.insert {

                    }

            this add UserModel.update({}) { Row() }

            this add UserModel.update(Row())

            commit()
        }
*/

        val transaction = newAutoCommitTransaction {
            this add UserModel
                .select(UserModel.id, UserModel.login, UserModel.password)
                .where { UserModel.login eq data.login }
                .named("select-user")
        }

        redisEvent.switchOnDb(transaction, redisEvent.mutateState(AuthEventState.VERIFY, data))
    }

    suspend fun verify(redisEvent: RedisEvent) {
        val data = redisEvent.parseDbSelect()["select-user"]?.firstOrNull()
        val authDto = redisEvent.parseState<AuthInputDto>()

        if (data == null || authDto == null) {
            redisEvent.switchOnApi(ForbiddenException())
            return
        }

        if (data[UserModel.password] == authDto.password) {
            redisEvent.switchOnApi(TokenOutputDto("", ""))
        } else {
            redisEvent.switchOnApi(ForbiddenException())
        }
    }
}