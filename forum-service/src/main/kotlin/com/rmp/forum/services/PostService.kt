package com.rmp.forum.services

import com.rmp.forum.actions.CreatePostFsm
import com.rmp.forum.actions.ShareAchievementFsm
import com.rmp.forum.dto.*
import com.rmp.lib.exceptions.BadRequestException
import com.rmp.lib.exceptions.ForbiddenException
import com.rmp.lib.exceptions.InternalServerException
import com.rmp.lib.shared.conf.AppConf
import com.rmp.lib.shared.dto.Response
import com.rmp.lib.shared.modules.dish.DishModel
import com.rmp.lib.shared.modules.forum.PostModel
import com.rmp.lib.shared.modules.forum.UserUpvoteModel
import com.rmp.lib.shared.modules.user.UserModel
import com.rmp.lib.utils.files.FilesUtil
import com.rmp.lib.utils.korm.Row
import com.rmp.lib.utils.korm.column.eq
import com.rmp.lib.utils.korm.column.inList
import com.rmp.lib.utils.korm.column.inRange
import com.rmp.lib.utils.korm.column.lessEq
import com.rmp.lib.utils.korm.insert
import com.rmp.lib.utils.korm.query.builders.filter.and
import com.rmp.lib.utils.korm.query.builders.filter.or
import com.rmp.lib.utils.log.Logger
import com.rmp.lib.utils.redis.RedisEvent
import com.rmp.lib.utils.redis.fsm.FsmService
import org.kodein.di.DI
import org.kodein.di.instance
import kotlin.math.roundToLong

class PostService(di: DI) : FsmService(di) {
    private val subscribeService: SubscribeService by instance()

    private fun Row.toDto(): PostDto =
        PostDto(
            this[PostModel.id],
            this[UserModel.id],
            this[UserModel.nickname],
            this[UserModel.isMale],
            this[PostModel.upVotes],
            this[PostModel.image],
            this[PostModel.text],
            this[PostModel.title]
        )



    suspend fun fetchFeed(redisEvent: RedisEvent) {
        val user = redisEvent.authorizedUser ?: throw ForbiddenException()

        newTransaction(redisEvent) {}

        val mostPopularUsers = subscribeService.getMostPopularUsers(redisEvent).filter { it != user.id }
        val users = subscribeService.getUserSubscriptions(redisEvent, user.id) + mostPopularUsers

        val lastWeekPosts = System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000

        val posts = autoCommitTransaction(redisEvent) {
            this add PostModel.select().join(UserModel).where {
                PostModel.timestamp.inRange(lastWeekPosts, System.currentTimeMillis()) and (PostModel.authorId inList users)
            }
        }[PostModel] ?: listOf()

        redisEvent.switchOnApi(PostListDto(posts.map { it.toDto() }.shuffled()))
    }

    suspend fun getUserPosts(redisEvent: RedisEvent, userId: Long): List<PostDto> {
        val posts = transaction(redisEvent) {
            this add PostModel.select().join(UserModel).where { PostModel.authorId eq userId }
        }[PostModel] ?: listOf()

        return posts.map { it.toDto() }
    }

    suspend fun createPost(redisEvent: RedisEvent) {
        val user = redisEvent.authorizedUser ?: throw ForbiddenException()
        val postData = redisEvent.parseData<CreatePostDto>() ?: throw BadRequestException("Bad Request")

        val imageName = if (postData.image != null) {
            if (postData.imageName != null)
                FilesUtil.buildName(postData.imageName)
            else throw BadRequestException("You must provide base64 encoded image")
        } else postData.imageName

        val createdAt = System.currentTimeMillis()
        newTransaction(redisEvent) {
            this add PostModel.insert {
                it[title] = postData.title
                it[text] = postData.text
                it[image] = imageName
                it[authorId] = user.id
                it[timestamp] = createdAt
            }.named("insert-post")
        }["insert-post"]?.firstOrNull() ?: throw InternalServerException("Failed to create post")

        val post = transaction(redisEvent) {
            this add PostModel.select().join(UserModel).where { (PostModel.timestamp eq createdAt) and (PostModel.authorId eq user.id) }
        }[PostModel]?.firstOrNull() ?: throw InternalServerException("Failed to create post")

        if (imageName != null && postData.image != null) {
            FilesUtil.upload(postData.image, imageName)
        }

        autoCommitTransaction(redisEvent) {}

        redisEvent.switchOnApi(post.toDto())
    }

    suspend fun upvotePost(redisEvent: RedisEvent) {
        val user = redisEvent.authorizedUser ?: throw ForbiddenException()
        val upvotePostDto = redisEvent.parseData<UpvotePostDto>() ?: throw BadRequestException("Bad request")
        val postId = upvotePostDto.id

        val upvoteExist = newAutoCommitTransaction(redisEvent) {
            this add UserUpvoteModel.select().where { (UserUpvoteModel.postId eq postId) and (UserUpvoteModel.userId eq user.id) }
        }[UserUpvoteModel]?.firstOrNull() != null

        if (upvoteExist == upvotePostDto.upvote) throw BadRequestException("Bad request")

        newAutoCommitTransaction(redisEvent) {
            if (upvotePostDto.upvote) {
                this add UserUpvoteModel.insert {
                    it[UserUpvoteModel.postId] = postId
                    it[userId] = user.id
                }.named("insert-upvote-log")

                this add PostModel.update(PostModel.id eq postId) {
                    PostModel.upVotes += 1
                }
            } else {
                this add UserUpvoteModel.delete((UserUpvoteModel.postId eq postId) and (UserUpvoteModel.userId eq user.id))
                this add PostModel.update(PostModel.id eq postId) {
                    PostModel.upVotes -= 1
                }
            }
        }

        redisEvent.switchOnApi(Response(true, if (upvotePostDto.upvote) "Upvoted" else "Downvoted"))
    }


    suspend fun shareAchievement(redisEvent: RedisEvent) {
        val data = redisEvent.parseData<ShareAchievementInputDto>() ?: throw BadRequestException("Bad data provided")

        val achievementTitle = when (data.achievementType) {
            1 -> "питанию"
            2 -> "воде"
            3 -> "cну"
            4 -> "шагам"
            else -> throw BadRequestException("Bad type provided")
        }

        val dayLabel = if (data.current > 1) "дней" else "дня"

        val postData = CreatePostDto(
            "Какой же я крутой",
            "Мне удалось соблюдать норму по $achievementTitle на протяжении ${data.current} $dayLabel. И попасть в топ ${data.percentage}% пользователей!"
        )

        redisEvent
            .copyId("create-post")
            .switchOn(
                postData,
                AppConf.redis.forum,
                redisEvent.mutate(CreatePostFsm.CreatePostEventState.INIT)
            )
    }

    suspend fun shareMenu(redisEvent: RedisEvent) {
        val user = redisEvent.authorizedUser ?: throw ForbiddenException()
        val data = redisEvent.parseData<ShareMenuInputDto>() ?: throw BadRequestException("Bad data provided")

        val dish = newAutoCommitTransaction(redisEvent) {
            this add DishModel.select().where {
                ((DishModel.private eq false) or (DishModel.author eq user.id)) and (DishModel.id eq data.dishId)
            }
        }[DishModel]?.firstOrNull() ?: throw BadRequestException("Bad dish id provided")


        val postData = CreatePostDto(
            title = "Новый рецепт!",
            imageName = dish[DishModel.imageUrl],
            text = "Посмотри какое классное блюдо можно приготовить: \n\n" +
                    dish[DishModel.name] + "\n" +
                    dish[DishModel.description] + "\n" +
                    "Калории: ${dish[DishModel.calories].roundToLong()}, Белки: ${dish[DishModel.protein].roundToLong()}, Жиры: ${dish[DishModel.fat].roundToLong()}, Углеводы: ${dish[DishModel.carbohydrates].roundToLong()}"
        )

        redisEvent
            .copyId("create-post")
            .switchOn(
                postData,
                AppConf.redis.forum,
                redisEvent.mutate(CreatePostFsm.CreatePostEventState.INIT)
            )
    }
}