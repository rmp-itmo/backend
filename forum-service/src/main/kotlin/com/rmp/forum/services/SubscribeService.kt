package com.rmp.forum.services

import com.rmp.forum.dto.SubscribeDto
import com.rmp.lib.exceptions.BadRequestException
import com.rmp.lib.exceptions.ForbiddenException
import com.rmp.lib.shared.dto.Response
import com.rmp.lib.shared.modules.forum.UserSubsModel
import com.rmp.lib.shared.modules.user.UserModel
import com.rmp.lib.utils.korm.column.eq
import com.rmp.lib.utils.korm.insert
import com.rmp.lib.utils.korm.query.builders.OrderBy
import com.rmp.lib.utils.korm.query.builders.filter.and
import com.rmp.lib.utils.redis.RedisEvent
import com.rmp.lib.utils.redis.fsm.FsmService
import org.kodein.di.DI

class SubscribeService(di: DI) : FsmService(di) {
    suspend fun manageSubscription(redisEvent: RedisEvent) {
        val user = redisEvent.authorizedUser ?: throw ForbiddenException()
        val sub = redisEvent.parseData<SubscribeDto>() ?: throw BadRequestException("Bad data provided")

        val alreadySubscribed = newAutoCommitTransaction(redisEvent) {
            this add UserSubsModel.select().where { (UserSubsModel.userId eq sub.targetId) and (UserSubsModel.sub eq user.id) }
        }[UserSubsModel]?.firstOrNull() != null

        if (alreadySubscribed == sub.sub) throw BadRequestException("Bad request")

        newAutoCommitTransaction(redisEvent) {
            if (sub.sub) {
                this add UserSubsModel.insert {
                    it[UserSubsModel.sub] = user.id
                    it[userId] = sub.targetId
                }.named("set-sub")
                this add UserModel.update(UserModel.id eq sub.targetId) {
                    UserModel.subsCount += 1
                }
            } else {
                this add UserModel.update(UserModel.id eq sub.targetId) {
                    UserModel.subsCount -= 1
                }
                this add UserSubsModel.delete((UserSubsModel.userId eq sub.targetId) and (UserSubsModel.sub eq user.id))
            }
        }

        redisEvent.switchOnApi(Response(true, if (sub.sub) "Subscribed" else "Unsubscribed"))
    }

    suspend fun getUserSubscriptions(redisEvent: RedisEvent, userId: Long): List<Long> =
        (transaction(redisEvent) {
            this add UserSubsModel.select(UserSubsModel.userId).where { UserSubsModel.sub eq userId }
        }[UserSubsModel] ?: emptyList()).map {
            it[UserSubsModel.userId]
        }

    suspend fun getMostPopularUsers(redisEvent: RedisEvent): List<Long> =
        (transaction(redisEvent) {
            this add UserModel.select(UserModel.id).orderBy(UserModel.subsCount, OrderBy.DESC).limit(10)
        }[UserModel] ?: emptyList()).map {
            it[UserModel.id]
        }
}