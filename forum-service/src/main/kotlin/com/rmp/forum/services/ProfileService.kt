package com.rmp.forum.services

import com.rmp.forum.dto.FetchProfileDto
import com.rmp.forum.dto.ProfileDto
import com.rmp.lib.exceptions.BadRequestException
import com.rmp.lib.exceptions.ForbiddenException
import com.rmp.lib.shared.modules.user.UserModel
import com.rmp.lib.utils.korm.column.eq
import com.rmp.lib.utils.korm.column.inList
import com.rmp.lib.utils.redis.RedisEvent
import com.rmp.lib.utils.redis.fsm.FsmService
import org.kodein.di.DI
import org.kodein.di.instance

class ProfileService(di: DI) : FsmService(di) {
    private val postService: PostService by instance()
    private val subscribeService: SubscribeService by instance()

    private suspend fun getProfile(redisEvent: RedisEvent, userId: Long, authorizedUser: Long? = null): ProfileDto {
        val data = newTransaction(redisEvent) {
            this add UserModel.select().where { UserModel.id eq userId }
        }

        val userData = data[UserModel]?.firstOrNull() ?: throw BadRequestException("User not found")
        val posts = postService.getUserPosts(redisEvent, userId, authorizedUser)
        val subscriptionsIds = subscribeService.getUserSubscriptions(redisEvent, userId)

        val isSubscribed = if (authorizedUser != null) {
            subscribeService.getUserSubscriptions(redisEvent, authorizedUser).contains(userId)
        } else {
            false
        }
        val subscriptions = transaction(redisEvent) {
            this add UserModel.select(UserModel.id, UserModel.nickname).where { UserModel.id inList subscriptionsIds }
        }[UserModel] ?: listOf()

        autoCommitTransaction(redisEvent) {}

        return ProfileDto(
            userData[UserModel.id],
            userData[UserModel.nickname],
            userData[UserModel.subsCount],
            subscriptions
                .groupBy { it[UserModel.id] }
                .map { (id, user) -> id to user.first()[UserModel.nickname] }
                .toMap(),
            userData[UserModel.registrationDate],
            posts.reversed(),
            isSubscribed
        )
    }

    suspend fun getProfile(redisEvent: RedisEvent) {
        val userId = redisEvent.parseData<FetchProfileDto>()?.id ?: throw BadRequestException("User not found")

        val profile = getProfile(redisEvent, userId, redisEvent.authorizedUser?.id)

        redisEvent.switchOnApi(profile)
    }

    suspend fun getMe(redisEvent: RedisEvent) {
        val user = redisEvent.authorizedUser ?: throw ForbiddenException()

        val profile = getProfile(redisEvent, user.id)

        redisEvent.switchOnApi(profile)
    }
}