package com.rmp.tm

import com.rmp.lib.shared.conf.AppConf
import com.rmp.lib.shared.modules.diet.DietDishLogModel
import com.rmp.lib.shared.modules.diet.DietWaterLogModel
import com.rmp.lib.shared.modules.dish.DishModel
import com.rmp.lib.shared.modules.dish.DishTypeModel
import com.rmp.lib.shared.modules.dish.UserMenuItem
import com.rmp.lib.shared.modules.dish.UserMenuModel
import com.rmp.lib.shared.modules.forum.PostModel
import com.rmp.lib.shared.modules.forum.UserSubsModel
import com.rmp.lib.shared.modules.forum.UserUpvoteModel
import com.rmp.lib.shared.modules.paprika.CacheModel
import com.rmp.lib.shared.modules.paprika.CacheToDishModel
import com.rmp.lib.shared.modules.sleep.SleepQualityModel
import com.rmp.lib.shared.modules.stat.GraphCacheModel
import com.rmp.lib.shared.modules.target.TargetLogModel
import com.rmp.lib.shared.modules.training.TrainingIntensityModel
import com.rmp.lib.shared.modules.training.TrainingTypeModel
import com.rmp.lib.shared.modules.training.UserTrainingLogModel
import com.rmp.lib.shared.modules.user.*
import com.rmp.lib.utils.kodein.bindSingleton
import com.rmp.lib.utils.korm.DbType
import com.rmp.lib.utils.korm.TableRegister
import com.rmp.lib.utils.korm.query.BatchQuery
import com.rmp.lib.utils.korm.query.QueryDto
import com.rmp.lib.utils.metrics.MetricsProvider
import com.rmp.lib.utils.redis.*
import com.rmp.tm.conf.ServiceConf
import com.rmp.tm.korm.TransactionManager
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import kotlinx.coroutines.*
import org.kodein.di.DI

val prometheusMeterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)

val kodein = DI {
    bindSingleton { PubSubService(AppConf.redis.db, prometheusMeterRegistry, it) }
}

fun main() {
    TransactionManager.init {
        jdbcUrl = ServiceConf.dbConf.jdbcUrl
        driverClassName = ServiceConf.dbConf.driverClassName
        username = ServiceConf.dbConf.username
        password = ServiceConf.dbConf.password
    }

    TableRegister.register(DbType.PGSQL, UserGoalTypeModel, UserActivityLevelModel)
    TableRegister.register(DbType.PGSQL, UserModel, UserLoginModel)
    TableRegister.register(DbType.PGSQL, DishTypeModel, DishModel)
    TableRegister.register(DbType.PGSQL, CacheModel, CacheToDishModel)
    TableRegister.register(DbType.PGSQL, DietDishLogModel, DietWaterLogModel)
    TableRegister.register(DbType.PGSQL, UserMenuModel, UserMenuItem)
    TableRegister.register(DbType.PGSQL, SleepQualityModel)
    TableRegister.register(DbType.PGSQL, UserSleepModel)
    TableRegister.register(DbType.PGSQL, UserHeartLogModel)
    TableRegister.register(DbType.PGSQL, UserStepsLogModel)
    TableRegister.register(DbType.PGSQL, UserAchievementsModel)
    TableRegister.register(DbType.PGSQL, GraphCacheModel)
    TableRegister.register(DbType.PGSQL, TrainingTypeModel, TrainingIntensityModel, UserTrainingLogModel)
    TableRegister.register(DbType.PGSQL, UserSubsModel, PostModel, UserUpvoteModel, TargetLogModel)

    runBlocking {
        TransactionManager.initTables(
            forceRecreate = true,
            excludedFromRecreation = mutableSetOf(
                DishModel, DishTypeModel,
                UserGoalTypeModel, UserActivityLevelModel,
                UserModel, UserAchievementsModel, TargetLogModel,
                SleepQualityModel, TrainingTypeModel, TrainingIntensityModel,
                UserSubsModel, PostModel, UserUpvoteModel
            )
        ) {

            // User goals
//        this add UserGoalTypeModel.insert {
//            it[name] = "Lose"
//            it[coefficient] = 0.85F
//        }.named("insert-lose-goal-type")
//
//        this add UserGoalTypeModel.insert {
//            it[name] = "Maintain"
//            it[coefficient] = 1.0F
//        }.named("insert-maintain-goal-type")
//
//        this add UserGoalTypeModel.insert {
//            it[name] = "Gain"
//            it[coefficient] = 1.15F
//        }.named("insert-gain-goal-type")

            // User activity
//        this add UserActivityLevelModel.insert {
//            it[name] = "Low"
//            it[caloriesCoefficient] = 1.2F
//            it[waterCoefficient] = 0.03F
//            it[defaultSteps] = 6000
//        }.named("insert-low-activity-type")
//
//        this add UserActivityLevelModel.insert {
//            it[name] = "Medium"
//            it[caloriesCoefficient] = 1.55F
//            it[waterCoefficient] = 0.04F
//            it[defaultSteps] = 8000
//        }.named("insert-medium-activity-type")
//
//        this add UserActivityLevelModel.insert {
//            it[name] = "High"
//            it[caloriesCoefficient] = 1.75F
//            it[waterCoefficient] = 0.05F
//            it[defaultSteps] = 10000
//        }.named("insert-high-activity-type")

            // User
//        this add UserModel.insert {
//            it[name] = "User"
//            it[email] = "login@test.test"
//            it[password] = CryptoUtil.hash("password")
//            it[waterTarget] = 1.2
//            it[caloriesTarget] = 4.1
//            it[height] = 185.0F
//            it[weight] = 73.0F
//            it[activityLevel] = 1
//            it[goalType] = 1
//            it[isMale] = true
//            it[age] = 25
//            it[nickname] = "nickname"
//            it[stepsTarget] = 6000
//            it[registrationDate] = 20230304
//        }.named("insert-base-user")
//
//        this add UserAchievementsModel.insert {
//            it[userId] = 1
//            it[water] = 1
//            it[calories] = 1
//            it[sleep] = 1
//            it[steps] = 1
//        }.named("streaks")

//        this add SleepQualityModel.batchInsert(SleepQuality.entries) { item, idx ->
//            this[SleepQualityModel.id] = idx + 1L
//            this[SleepQualityModel.name] = item.name
//        }.named("add-sleep-quality")
//
//        this add TargetLogModel.insert {
//            it[userId] = 1
//            it[date] = 20250324
//            it[waterTarget] = 1.8
//            it[caloriesTarget] = 2000.0
//            it[stepTarget] = 10000
//            it[sleepTarget] = 9.0f
//        }.named("insert-target-log")

            // Trainings types
//        this add TrainingTypeModel.insert {
//            it[name] = "Плавание"
//            it[coefficient] = 1.3
//        }.named("add-training-type-swimming")
//
//        this add TrainingTypeModel.insert {
//            it[name] = "Велосипед"
//            it[coefficient] = 1.4
//        }.named("add-training-type-cycle")
//
//        this add TrainingTypeModel.insert {
//            it[name] = "Бег"
//            it[coefficient] = 1.5
//        }.named("add-training-type-running")
//
//        // Trainings intensive
//        this add TrainingIntensityModel.insert {
//            it[name] = "Высокая"
//            it[coefficient] = 1.3
//        }.named("add-training-intensity-high")
//
//        this add TrainingIntensityModel.insert {
//            it[name] = "Средняя"
//            it[coefficient] = 1.4
//        }.named("add-training-intensity-mid")
//
//        this add TrainingIntensityModel.insert {
//            it[name] = "Низкая"
//            it[coefficient] = 1.5
//        }.named("add-training-intensity-low")
        }

        coroutineScope {
            val handler = object : RedisSubscriber() {
                override fun onMessage(redisEvent: RedisEvent?, channel: String, message: String) {
                    if (redisEvent == null) {
                        throw Exception("Unknown event in $channel ($message)")
                    }

                    val tryDecode = redisEvent.parseData<QueryDto>(silent = true) ?: redisEvent.parseData<BatchQuery>()

                    if (tryDecode == null) return

                    launch(context = Dispatchers.IO.limitedParallelism(Int.MAX_VALUE)) {
                        when (tryDecode) {
                            is BatchQuery -> {
                                TransactionManager.process(redisEvent, tryDecode)
                            }

                            is QueryDto -> {
                                TransactionManager.process(redisEvent, BatchQuery(mutableMapOf("simple" to tryDecode)))
                            }
                        }
                    }
                }
            }

            withContext(Dispatchers.IO) {
                launch {
                    MetricsProvider.start(prometheusMeterRegistry)
                }

                subscribe(handler, true, AppConf.redis.db)
            }
        }
    }
}
