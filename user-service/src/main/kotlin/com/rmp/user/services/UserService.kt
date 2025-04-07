package com.rmp.user.services

import com.rmp.lib.exceptions.*
import com.rmp.lib.shared.conf.AppConf
import com.rmp.lib.shared.dto.CurrentCaloriesOutputDto
import com.rmp.lib.shared.dto.DishLogCheckDto
import com.rmp.lib.shared.dto.target.TargetUpdateLogDto
import com.rmp.lib.shared.modules.user.*
import com.rmp.lib.utils.korm.column.Column
import com.rmp.lib.utils.korm.column.eq
import com.rmp.lib.utils.korm.column.less
import com.rmp.lib.utils.korm.insert
import com.rmp.lib.utils.korm.references.JoinType
import com.rmp.lib.utils.redis.RedisEvent
import com.rmp.lib.utils.redis.fsm.FsmService
import com.rmp.lib.utils.security.bcrypt.CryptoUtil
import com.rmp.user.actions.create.UserCreateEventState
import com.rmp.user.actions.update.UserUpdateEventState
import com.rmp.user.dto.*
import com.rmp.user.dto.sleep.UserSleepDto
import com.rmp.user.dto.summary.UserSummaryOutputDto
import com.rmp.user.dto.streaks.AchievementsOutputDto
import com.rmp.user.dto.streaks.AchievementDto
import org.kodein.di.DI
import kotlin.math.roundToInt
import kotlin.math.round

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

        val user = newTransaction(redisEvent) {
            this add UserModel
                .insert {
                    it[name] = data.name
                    it[nickname] = "${data.name}#${System.currentTimeMillis()}"
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
                    it[registrationDate] = data.registrationDate
                }.named("insert-user")
        }["insert-user"]?.firstOrNull() ?: throw InternalServerException("Insert failed")

        val update = transaction(redisEvent) {
            this add UserModel
                .update(UserModel.id eq user[UserModel.id]) {
                    UserModel.nickname.set("${data.name}-${user[UserModel.id]}")
                }
            this add UserAchievementsModel
                .insert {
                    it[userId] = user[UserModel.id]
                    it[sleep] = 0
                    it[steps] = 0
                    it[UserAchievementsModel.water] = 0
                    it[UserAchievementsModel.calories] = 0
                }.named("insert-streak-row")
        }

        val count = update[UserModel]?.firstOrNull()?.get(UserModel.updateCount)
        if (count == null || count < 1) throw InternalServerException("Failed to update")

        autoCommitTransaction(redisEvent) {}

        redisEvent
            .copyId("target-update-log")
            .switchOn(TargetUpdateLogDto(data.registrationDate, user[UserModel.id]), AppConf.redis.stat,
                redisEvent.mutate(UserCreateEventState.LOG))

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

    private suspend fun checkEmail(redisEvent: RedisEvent, email: String?) {
        if (email != null) {
            val user = newAutoCommitTransaction(redisEvent) {
                this add UserModel
                    .select(UserModel.id)
                    .where { UserModel.email eq  email}
            }[UserModel]?.firstOrNull()

            if (user != null) {
                throw DoubleRecordException("User already exists")
            }
        }
    }

    suspend fun updateUser(redisEvent: RedisEvent) {

        val authUser = redisEvent.authorizedUser ?: throw ForbiddenException()
        val data = redisEvent.parseData<UserUpdateDto>() ?: throw BadRequestException("Invalid data provided")
        val userData = getUserInfo(redisEvent, authUser.id, false)

        checkEmail(redisEvent, data.email)

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

        val update = newAutoCommitTransaction(redisEvent) {
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
                }
        }

        val count = update[UserModel]?.firstOrNull()?.get(UserModel.updateCount)
        if (count == null || count < 1) throw InternalServerException("Failed to update")

        redisEvent
            .copyId("target-update-log")
            .switchOn(TargetUpdateLogDto(data.date, authUser.id), AppConf.redis.stat,
                redisEvent.mutate(UserUpdateEventState.LOG))

        redisEvent.switchOnApi(UserCreateOutputDto(authUser.id))
    }

    private suspend fun checkNickname(redisEvent: RedisEvent, nick: String): Boolean {
        val user = newAutoCommitTransaction(redisEvent) {
            this add UserModel
                .select(UserModel.id)
                .where { UserModel.nickname eq nick }
        }[UserModel]?.firstOrNull()
        if (user != null) {
            throw DoubleRecordException("This nickname already in use")
        } else {
            return true
        }
    }

    private suspend fun getUserInfo(redisEvent: RedisEvent, id: Long, isOutput: Boolean): UserOutputDto {
        val select = newAutoCommitTransaction(redisEvent) {
            this add UserModel
                .select(
                    UserModel.id, UserModel.name, UserModel.email, UserModel.age, UserModel.height,
                    UserModel.weight, UserModel.isMale, UserModel.caloriesStreak, UserModel.waterStreak,
                    UserModel.caloriesTarget, UserModel.waterTarget, UserModel.nickname, UserModel.stepsTarget,
                    UserModel.stepsCount, UserModel.waterCurrent, UserModel.caloriesCurrent,
                    UserActivityLevelModel.name, UserActivityLevelModel.caloriesCoefficient,
                    UserActivityLevelModel.waterCoefficient, UserGoalTypeModel.name,
                    UserGoalTypeModel.coefficient
                ).where { UserModel.id eq id }
                .join(UserActivityLevelModel, JoinType.INNER, UserActivityLevelModel.id eq UserModel.activityLevel)
                .join(UserGoalTypeModel, JoinType.INNER, UserGoalTypeModel.id eq UserModel.goalType)
        }[UserModel]?.firstOrNull() ?: throw BadRequestException("User does not exist")

        val heartRate = newAutoCommitTransaction(redisEvent) {
            this add UserHeartLogModel
                .select(UserHeartLogModel.heartRate)
                .where { UserHeartLogModel.user eq id }
        }[UserHeartLogModel]?.lastOrNull()?.get(UserHeartLogModel.heartRate)

        return UserOutputDto(
            select[UserModel.id], select[UserModel.name], select[UserModel.email],
            select[UserModel.height], select[UserModel.weight], select[UserActivityLevelModel.name],
            select[UserGoalTypeModel.name], select[UserModel.isMale], select[UserModel.age],
            select[UserModel.waterTarget], select[UserModel.caloriesTarget],
            select[UserModel.waterStreak], select[UserModel.caloriesStreak],
            select[UserModel.nickname],
            select[UserModel.stepsTarget],
            select[UserModel.stepsCount],
            heartRate, select[UserModel.waterCurrent], select[UserModel.caloriesCurrent],
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

    suspend fun updateCalories(redisEvent: RedisEvent) {
        // Internal exception по причине того что этот метод вызывается исключительно изнутри (DishService.addMenuItem \ .uploadDish),
        // и если сюда пришло плохое тело - наша ошибка, а не клиента
        val user = redisEvent.authorizedUser ?: throw InternalServerException("Bad request")
        val data = redisEvent.parseData<DishLogCheckDto>() ?: throw InternalServerException("Bad request")

        // Продолжаем старую транзакцию открытую при изменении состояния меню в DishService
        val userData = transaction(redisEvent) {
            this add UserModel.select(UserModel.caloriesCurrent).where {
                UserModel.id eq user.id
            }
        }[UserModel]?.firstOrNull() ?: throw InternalServerException("Failed to fetch user")

        val currentCalories = userData[UserModel.caloriesCurrent]

        val newCalories = if (data.check) {
            currentCalories + data.calories
        } else {
            (currentCalories - data.calories).let { if (it < 0) 0.0 else it }
        }

        autoCommitTransaction(redisEvent) {
            this add UserModel.update(UserModel.id eq user.id) {
                this[UserModel.caloriesCurrent] = newCalories
            }
        }

        redisEvent.switchOnApi(CurrentCaloriesOutputDto(newCalories))
    }

    private suspend fun countPercentage(redisEvent: RedisEvent, items: List<Pair<Column<Int>, Int>>, countAll: Long): Map<Column<Int>, Int> {
        val countData = newAutoCommitTransaction(redisEvent) {
            items.forEachIndexed { idx, (column, value) ->
                this add UserAchievementsModel.select().where { column less value }.count("count-less-$idx")
            }
        }

        val result = items.mapIndexed { index, (column, _) ->
            val data = countData["count-less-$index"]?.firstOrNull() ?: throw InternalServerException("Count failed")
            column to ((data[UserAchievementsModel.entityCount].toDouble() / (countAll - 1).toDouble()) * 100).roundToInt()
        }.toMap()

        return result
    }

    suspend fun getAchievements(redisEvent: RedisEvent) {
        val user = redisEvent.authorizedUser ?: throw ForbiddenException()

        val userData = newAutoCommitTransaction(redisEvent) {
            this add UserAchievementsModel
                        .select(
                            UserModel.caloriesStreak, UserModel.waterStreak, UserModel.stepsStreak, UserModel.sleepStreak,
                            UserAchievementsModel.id, UserAchievementsModel.calories, UserAchievementsModel.water, UserAchievementsModel.sleep, UserAchievementsModel.steps
                        )
                        .join(UserModel)
                        .where { UserModel.id eq user.id }
        }[UserAchievementsModel]?.firstOrNull() ?: throw InternalServerException("Failed to fetch user")

        val usersCount = newAutoCommitTransaction(redisEvent) {
            this add UserModel.select().count("count-users")
        }["count-users"]?.firstOrNull() ?: throw InternalServerException("Count query failed")

        val items = listOf(
            UserAchievementsModel.calories to userData[UserAchievementsModel.calories],
            UserAchievementsModel.water to userData[UserAchievementsModel.water],
            UserAchievementsModel.steps to userData[UserAchievementsModel.steps],
            UserAchievementsModel.sleep to userData[UserAchievementsModel.sleep],
        )
        val percentage = countPercentage(redisEvent, items, usersCount[UserModel.entityCount])

        val currentStreaks = userData.unwrap(UserAchievementsModel)
        val updated = (userData[UserModel.caloriesStreak] > userData[UserAchievementsModel.calories]) ||
                (userData[UserModel.waterStreak] > userData[UserAchievementsModel.water]) ||
                (userData[UserModel.stepsStreak] > userData[UserAchievementsModel.steps]) ||
                (userData[UserModel.caloriesStreak] > userData[UserAchievementsModel.calories])

        if (userData[UserModel.caloriesStreak] > userData[UserAchievementsModel.calories]) {
            currentStreaks[UserAchievementsModel.calories] = userData[UserModel.caloriesStreak]
        }
        if (userData[UserModel.waterStreak] > userData[UserAchievementsModel.water]) {
            currentStreaks[UserAchievementsModel.water] = userData[UserModel.waterStreak]
        }
        if (userData[UserModel.stepsStreak] > userData[UserAchievementsModel.steps]) {
            currentStreaks[UserAchievementsModel.steps] = userData[UserModel.stepsStreak]
        }
        if (userData[UserModel.sleepStreak] > userData[UserAchievementsModel.sleep]) {
            currentStreaks[UserAchievementsModel.sleep] = userData[UserModel.sleepStreak]
        }

        if (updated) newAutoCommitTransaction(redisEvent) { this add UserAchievementsModel.update(currentStreaks) }

        val achievements = AchievementsOutputDto(
            calories = AchievementDto(
                current = userData[UserModel.caloriesStreak],
                max = currentStreaks[UserAchievementsModel.calories],
                percentage = percentage[UserAchievementsModel.calories]!!
            ),
            water = AchievementDto(
                current = userData[UserModel.waterStreak],
                max = currentStreaks[UserAchievementsModel.water],
                percentage = percentage[UserAchievementsModel.water]!!
            ),
            steps = AchievementDto(
                current = userData[UserModel.stepsStreak],
                max = currentStreaks[UserAchievementsModel.steps],
                percentage = percentage[UserAchievementsModel.steps]!!
            ),
            sleep = AchievementDto(
                current = userData[UserModel.sleepStreak],
                max = currentStreaks[UserAchievementsModel.sleep],
                percentage = percentage[UserAchievementsModel.sleep]!!
            ),
        )

        redisEvent.switchOnApi(achievements)
    }

    suspend fun getUserSummary(redisEvent: RedisEvent) {
        val authorizedUser = redisEvent.authorizedUser ?: throw ForbiddenException()
        val data = redisEvent.parseData<UserSleepDto>() ?: throw InternalServerException("Bad data provided")

        val user = getUserInfo(redisEvent, authorizedUser.id, isOutput = true)

        val glasses = round(user.waterCurrent / (user.waterTarget / 8))

        redisEvent.switchOnApi(
            UserSummaryOutputDto(
                caloriesTarget = user.caloriesTarget,
                caloriesCurrent = user.caloriesCurrent,
                waterTarget = user.waterTarget,
                waterCurrent = user.waterCurrent,
                stepsTarget = user.stepsTarget,
                stepsCurrent = user.stepsCount,
                sleepHours = data.hours,
                sleepMinutes = data.minutes,
                heartRate = user.heartRate,
                glassesOfWater = glasses
            )
        )
    }
}