package com.rmp.user.services

import com.rmp.lib.exceptions.BadRequestException
import com.rmp.lib.exceptions.ForbiddenException
import com.rmp.lib.exceptions.InternalServerException
import com.rmp.lib.shared.modules.user.UserActivityLevelTypeModel
import com.rmp.lib.shared.modules.user.UserGoalTypeModel
import com.rmp.lib.shared.modules.user.UserModel
import com.rmp.lib.utils.korm.column.eq
import com.rmp.lib.utils.korm.insert
import com.rmp.lib.utils.korm.references.JoinType
import com.rmp.lib.utils.redis.RedisEvent
import com.rmp.lib.utils.redis.fsm.FsmService
import com.rmp.lib.utils.security.bcrypt.CryptoUtil
import com.rmp.user.dto.UserCreateInputDto
import com.rmp.user.dto.UserCreateOutputDto
import com.rmp.user.dto.UserOutputDto
import org.kodein.di.DI

class UserService(di: DI): FsmService(di) {
    suspend fun createUser(redisEvent: RedisEvent) {
        val data = redisEvent.parseData<UserCreateInputDto>() ?: throw BadRequestException("Invalid data provided")

        checkEmail(redisEvent, data.email)

        val select = newAutoCommitTransaction(redisEvent) {
            this add UserActivityLevelTypeModel
                .select(UserActivityLevelTypeModel.caloriesCoefficient, UserActivityLevelTypeModel.waterCoefficient)
                .where { UserActivityLevelTypeModel.id eq  data.activityType}

            this add UserGoalTypeModel
                .select(UserGoalTypeModel.coefficient)
                .where { UserGoalTypeModel.id eq  data.goalType}
        }

        val activityCoefficients = select[UserActivityLevelTypeModel]?.firstOrNull()
            ?: throw BadRequestException("Invalid activity level type provided")

        val goalCoefficient = select[UserGoalTypeModel]?.firstOrNull()
            ?: throw BadRequestException("Invalid goal type provided")

        val targets = calculateTargets(
            data,
            goalCoefficient[UserGoalTypeModel.coefficient],
            activityCoefficients[UserActivityLevelTypeModel.caloriesCoefficient],
            activityCoefficients[UserActivityLevelTypeModel.waterCoefficient]
        )

        val user = newAutoCommitTransaction(redisEvent) {
            this add UserModel
                .insert {
                    it[name] = data.name
                    it[email] = data.email
                    it[password] = CryptoUtil.hash(data.password)
                    it[waterTarget] = targets.second
                    it[caloriesTarget] = targets.first
                    it[high] = data.high
                    it[weight] = data.weight
                    it[activityLevelType] = data.activityType
                    it[goalType] = data.goalType
                    it[isMale] = data.isMale
                    it[age] = data.age
                }.named("insert-user")
        }["insert-user"]?.firstOrNull() ?: throw InternalServerException("Insert failed")

        redisEvent.switchOnApi(UserCreateOutputDto(user[UserModel.id]))
    }

    private fun calculateTargets(
        user: UserCreateInputDto,
        goalCoefficient: Float,
        caloriesCoefficient: Float,
        waterCoefficient: Float,
    ): Pair<Double, Double>{
        // Формула Миффлина-Сан Жеора для BMR
        val bmr = if (user.isMale) {
            10 * user.weight + 6.25 * user.high - 5 * user.age + 5
        } else {
            10 * user.weight + 6.25 * user.high - 5 * user.age - 161
        }

        val calories = bmr * caloriesCoefficient * goalCoefficient
        val water = user.weight * waterCoefficient

        return Pair(calories, water.toDouble())
    }

    private suspend fun checkEmail(redisEvent: RedisEvent, email: String) {
        val user = newAutoCommitTransaction(redisEvent) {
            this add UserModel
                .select(UserModel.id)
                .where { UserModel.email eq  email}
        }[UserModel]?.firstOrNull()

        if (user != null) {
            throw BadRequestException("User already exists")
        }
    }


    suspend fun getUser(redisEvent: RedisEvent) {
        val user = redisEvent.authorizedUser ?: throw ForbiddenException()

        val select = newAutoCommitTransaction(redisEvent) {
            this add UserModel
                .select(
                    UserModel.id,
                    UserModel.name,
                    UserModel.email,
                    UserModel.age,
                    UserModel.high,
                    UserModel.weight,
                    UserModel.isMale,
                    UserModel.caloriesStreak,
                    UserModel.waterStreak,
                    UserModel.caloriesTarget,
                    UserModel.waterTarget,
                    UserActivityLevelTypeModel.name,
                    UserGoalTypeModel.name
                ).where { UserModel.id eq user.id }
                .join(UserActivityLevelTypeModel, JoinType.INNER, UserActivityLevelTypeModel.id eq UserModel.activityLevelType)
                .join(UserGoalTypeModel, JoinType.INNER, UserGoalTypeModel.id eq UserModel.goalType)
        }[UserModel]?.firstOrNull() ?: throw BadRequestException("User does not exist")

        redisEvent.switchOnApi(
            UserOutputDto(
                select[UserModel.id],
                select[UserModel.name],
                select[UserModel.email],
                select[UserModel.high],
                select[UserModel.weight],
                select[UserActivityLevelTypeModel.name],
                select[UserGoalTypeModel.name],
                select[UserModel.isMale],
                select[UserModel.age],
                select[UserModel.waterTarget],
                select[UserModel.caloriesTarget],
                select[UserModel.waterStreak],
                select[UserModel.caloriesStreak]
            )
        )
    }
}