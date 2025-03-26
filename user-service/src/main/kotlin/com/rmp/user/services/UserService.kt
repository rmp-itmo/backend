package com.rmp.user.services

import com.rmp.lib.exceptions.BadRequestException
import com.rmp.lib.exceptions.ForbiddenException
import com.rmp.lib.exceptions.InternalServerException
import com.rmp.lib.shared.modules.user.UserActivityLevelModel
import com.rmp.lib.shared.modules.user.UserGoalTypeModel
import com.rmp.lib.shared.modules.user.UserModel
import com.rmp.lib.utils.korm.column.eq
import com.rmp.lib.utils.korm.insert
import com.rmp.lib.utils.korm.references.JoinType
import com.rmp.lib.utils.redis.RedisEvent
import com.rmp.lib.utils.redis.fsm.FsmService
import com.rmp.lib.utils.security.bcrypt.CryptoUtil
import com.rmp.user.dto.*
import org.kodein.di.DI

class UserService(di: DI): FsmService(di) {
    suspend fun createUser(redisEvent: RedisEvent) {
        val data = redisEvent.parseData<UserCreateInputDto>() ?: throw BadRequestException("Invalid data provided")

        checkEmail(redisEvent, data.email)

        val select = newAutoCommitTransaction(redisEvent) {
            this add UserActivityLevelModel
                .select(
                    UserActivityLevelModel.caloriesCoefficient,
                    UserActivityLevelModel.waterCoefficient,
                    UserActivityLevelModel.defaultSteps)
                .where { UserActivityLevelModel.id eq  data.activityType}

            this add UserGoalTypeModel
                .select(UserGoalTypeModel.coefficient)
                .where { UserGoalTypeModel.id eq  data.goalType}
        }

        val activityCoefficients = select[UserActivityLevelModel]?.firstOrNull()
            ?: throw BadRequestException("Invalid activity level type provided")

        val goalCoefficient = select[UserGoalTypeModel]?.firstOrNull()
            ?: throw BadRequestException("Invalid goal type provided")

        val (calories, water) = calculateTargets(
            data.isMale,
            data.weight,
            data.height,
            data.age,
            goalCoefficient[UserGoalTypeModel.coefficient],
            activityCoefficients[UserActivityLevelModel.caloriesCoefficient],
            activityCoefficients[UserActivityLevelModel.waterCoefficient]
        )

        val user = newAutoCommitTransaction(redisEvent) {
            this add UserModel
                .insert {
                    it[name] = data.name
                    it[email] = data.email
                    it[password] = CryptoUtil.hash(data.password)
                    it[waterTarget] = water
                    it[caloriesTarget] = calories
                    it[height] = data.height
                    it[weight] = data.weight
                    it[activityLevel] = data.activityType
                    it[goalType] = data.goalType
                    it[isMale] = data.isMale
                    it[age] = data.age
                    it[stepsTarget] = activityCoefficients[UserActivityLevelModel.defaultSteps]
                }.named("insert-user")
        }["insert-user"]?.firstOrNull() ?: throw InternalServerException("Insert failed")

        newAutoCommitTransaction(redisEvent) {
            this add UserModel
                .update(UserModel.id eq user[UserModel.id]) {
                    UserModel.nickname.set("${data.name}-${user[UserModel.id]}")
                }.named("insert-user-nickname")
        }

        redisEvent.switchOnApi(UserCreateOutputDto(user[UserModel.id]))
    }


    private fun calculateTargets(
        isMale: Boolean,
        weight: Float,
        height: Float,
        age: Int,
        goalCoefficient: Float,
        caloriesCoefficient: Float,
        waterCoefficient: Float,
    ): Pair<Double, Double>{
        // Формула Миффлина-Сан Жеора для BMR
        val bmr = if (isMale) {
            10 * weight + 6.25 * height - 5 * age + 5
        } else {
            10 * weight + 6.25 * height - 5 * age - 161
        }

        val calories = bmr * caloriesCoefficient * goalCoefficient
        val water = weight * waterCoefficient.toDouble()

        return calories to water
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

    suspend fun updateUser(redisEvent: RedisEvent) {

        val authUser = redisEvent.authorizedUser ?: throw ForbiddenException()
        val data = redisEvent.parseData<UserUpdateDto>() ?: throw BadRequestException("Invalid data provided")
        val userData = getUserInfo(redisEvent, authUser.id, false)

        val activityTypeSelect = if (data.activityType != null) {
            newAutoCommitTransaction(redisEvent) {
                this add UserActivityLevelModel
                    .select(UserActivityLevelModel.id, UserActivityLevelModel.caloriesCoefficient, UserActivityLevelModel.waterCoefficient)
                    .where { UserActivityLevelModel.name eq data.activityType }
            }[UserActivityLevelModel]?.firstOrNull()
                ?: throw BadRequestException("Invalid activity level type provided")
        } else {
            null
        }

        val goalTypeSelect = if (data.goalType != null) {
            newAutoCommitTransaction(redisEvent) {
                this add UserGoalTypeModel
                    .select(UserGoalTypeModel.id, UserGoalTypeModel.coefficient)
                    .where { UserGoalTypeModel.name eq data.goalType }
            }[UserGoalTypeModel]?.firstOrNull()
                ?: throw BadRequestException("Invalid goal type provided")
        } else {
            null
        }

        val shouldRecalculate = listOf(
            data.isMale, data.age, data.weight, data.height, data.activityType, data.goalType
        ).any { it != null }

        val (calories, water) = if (shouldRecalculate) {
            calculateTargets(
                data.isMale ?: userData.isMale,
                data.weight ?: userData.weight,
                data.height ?: userData.height,
                data.age ?: userData.age,
                goalTypeSelect?.get(UserGoalTypeModel.coefficient) ?: userData.goalCoefficient!!,
                activityTypeSelect?.get(UserActivityLevelModel.caloriesCoefficient) ?: userData.caloriesCoefficient!!,
                activityTypeSelect?.get(UserActivityLevelModel.waterCoefficient) ?: userData.waterCoefficient!!,
            )
        } else {
            null to null
        }

        val nick = if (data.nickname != null && checkNickname(redisEvent, data.nickname)) {
            data.nickname
        } else {
            userData.nickName
        }

        val goal = if (data.goalType == null) {
            val selectCurrentGoal = newAutoCommitTransaction(redisEvent) {
                this add UserGoalTypeModel
                    .select(UserGoalTypeModel.id)
                    .where { UserGoalTypeModel.name eq userData.goalType }
            }[UserGoalTypeModel]?.firstOrNull()
                ?: throw BadRequestException("Invalid goal type provided")
            selectCurrentGoal[UserGoalTypeModel.id]
        } else {
            goalTypeSelect!![UserGoalTypeModel.id]
        }

        val activity =  if (data.activityType == null) {
            val selectCurrentActivity = newAutoCommitTransaction(redisEvent) {
                this add UserActivityLevelModel
                    .select(UserActivityLevelModel.id)
                    .where { UserActivityLevelModel.name eq userData.activityType }
            }[UserActivityLevelModel]?.firstOrNull()
                ?: throw BadRequestException("Invalid activity level type provided")
            selectCurrentActivity[UserActivityLevelModel.id]
        } else {
            activityTypeSelect!![UserActivityLevelModel.id]
        }

        newAutoCommitTransaction(redisEvent) {
            this add UserModel
                .update(UserModel.id eq authUser.id) {
                    UserModel.name.set(data.name?: userData.name)
                    UserModel.email.set(data.email?: userData.email)
                    UserModel.waterTarget.set(water?: userData.waterTarget)
                    UserModel.caloriesTarget.set(calories?: userData.caloriesTarget)
                    UserModel.height.set(data.height?: userData.height)
                    UserModel.weight.set(data.weight?: userData.weight)
                    UserModel.isMale.set(data.isMale ?: userData.isMale)
                    UserModel.age.set(data.age?: userData.age)
                    UserModel.nickname.set(nick)

                    if (data.password != null) {
                        UserModel.password.set(CryptoUtil.hash(data.password))
                    }
                    UserModel.activityLevel.set(activity)
                    UserModel.goalType.set(goal)
                }.named("update-user")
        }

        redisEvent.switchOnApi(UserCreateOutputDto(authUser.id))
    }


    private suspend fun checkNickname(redisEvent: RedisEvent, nick: String): Boolean {
        val user = newAutoCommitTransaction(redisEvent) {
            this add UserModel
                .select(UserModel.id)
                .where { UserModel.nickname eq nick }
        }[UserModel]?.firstOrNull()
        if (user != null) {
            throw BadRequestException("This nickname already in use")
        } else {
            return true
        }
    }

    suspend fun updateSteps(redisEvent: RedisEvent) {
        val user = redisEvent.authorizedUser?: throw ForbiddenException()
        val stepsDto = redisEvent.parseData<UserStepsUpdateDto>() ?: throw BadRequestException("Invalid steps value provided")

        newAutoCommitTransaction(redisEvent) {
            this add UserModel
            .update(UserModel.id eq user.id) {
                UserModel.stepsTarget.set(stepsDto.steps)
            }.named("update-user-steps")
        }
        redisEvent.switchOnApi(UserCreateOutputDto(user.id))
    }

    private suspend fun getUserInfo(redisEvent: RedisEvent, id: Long, isOutput: Boolean): UserOutputDto {
        val select = newAutoCommitTransaction(redisEvent) {
            this add UserModel
                .select(
                    UserModel.id, UserModel.name, UserModel.email, UserModel.age, UserModel.height,
                    UserModel.weight, UserModel.isMale, UserModel.caloriesStreak, UserModel.waterStreak,
                    UserModel.caloriesTarget, UserModel.waterTarget, UserModel.nickname, UserModel.stepsTarget,
                    UserActivityLevelModel.name, UserActivityLevelModel.caloriesCoefficient,
                    UserActivityLevelModel.waterCoefficient, UserGoalTypeModel.name,
                    UserGoalTypeModel.coefficient
                ).where { UserModel.id eq id }
                .join(UserActivityLevelModel, JoinType.INNER, UserActivityLevelModel.id eq UserModel.activityLevel)
                .join(UserGoalTypeModel, JoinType.INNER, UserGoalTypeModel.id eq UserModel.goalType)
        }[UserModel]?.firstOrNull() ?: throw BadRequestException("User does not exist")

        return UserOutputDto(
            select[UserModel.id], select[UserModel.name], select[UserModel.email],
            select[UserModel.height], select[UserModel.weight], select[UserActivityLevelModel.name],
            select[UserGoalTypeModel.name], select[UserModel.isMale], select[UserModel.age],
            select[UserModel.waterTarget], select[UserModel.caloriesTarget],
            select[UserModel.waterStreak], select[UserModel.caloriesStreak],
            select[UserModel.nickname],
            select[UserModel.stepsTarget],
            waterCoefficient = if (!isOutput) select[UserActivityLevelModel.waterCoefficient] else null,
            caloriesCoefficient = if (!isOutput) select[UserActivityLevelModel.caloriesCoefficient] else null,
            goalCoefficient = if (!isOutput) select[UserGoalTypeModel.coefficient] else null
        )
    }

    suspend fun getUser(redisEvent: RedisEvent) {
        val user = redisEvent.authorizedUser ?: throw ForbiddenException()

        val userData = getUserInfo(redisEvent, user.id, true)

        redisEvent.switchOnApi(userData)
    }
}