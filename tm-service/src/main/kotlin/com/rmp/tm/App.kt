package com.rmp.tm

import com.rmp.lib.shared.conf.AppConf
import com.rmp.lib.shared.modules.diet.DietDishLogModel
import com.rmp.lib.shared.modules.diet.DietWaterLogModel
import com.rmp.lib.shared.modules.dish.DishModel
import com.rmp.lib.shared.modules.dish.DishTypeModel
import com.rmp.lib.shared.modules.dish.UserMenuItem
import com.rmp.lib.shared.modules.dish.UserMenuModel
import com.rmp.lib.shared.modules.paprika.CacheModel
import com.rmp.lib.shared.modules.paprika.CacheToDishModel
import com.rmp.lib.shared.modules.user.UserActivityLevelModel
import com.rmp.lib.shared.modules.user.UserGoalTypeModel
import com.rmp.lib.shared.modules.user.UserLoginModel
import com.rmp.lib.shared.modules.user.UserModel
import com.rmp.lib.utils.kodein.bindSingleton
import com.rmp.lib.utils.korm.DbType
import com.rmp.lib.utils.korm.TableRegister
import com.rmp.lib.utils.korm.insert
import com.rmp.lib.utils.korm.query.BatchQuery
import com.rmp.lib.utils.korm.query.QueryDto
import com.rmp.lib.utils.redis.PubSubService
import com.rmp.lib.utils.redis.RedisEvent
import com.rmp.lib.utils.redis.RedisSubscriber
import com.rmp.lib.utils.redis.subscribe
import com.rmp.lib.utils.security.bcrypt.CryptoUtil
import com.rmp.tm.conf.ServiceConf
import com.rmp.tm.korm.TransactionManager
import kotlinx.coroutines.*
import org.kodein.di.DI
import org.kodein.di.instance

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

    TransactionManager.initTables(
        forceRecreate = true,
        excludedFromRecreation = mutableSetOf(DishModel, DishTypeModel)
    ) {

        // User goals
        this add UserGoalTypeModel.insert {
            it[name] = "Lose"
            it[coefficient] = 0.85F
        }.named("insert-lose-goal-type")

        this add UserGoalTypeModel.insert {
            it[name] = "Maintain"
            it[coefficient] = 1.0F
        }.named("insert-maintain-goal-type")

        this add UserGoalTypeModel.insert {
            it[name] = "Gain"
            it[coefficient] = 1.15F
        }.named("insert-gain-goal-type")

        // User activity
        this add UserActivityLevelModel.insert {
            it[name] = "Low"
            it[caloriesCoefficient] = 1.2F
            it[waterCoefficient] = 0.03F
            it[defaultSteps] = 6000
        }.named("insert-low-activity-type")

        this add UserActivityLevelModel.insert {
            it[name] = "Medium"
            it[caloriesCoefficient] = 1.55F
            it[waterCoefficient] = 0.04F
            it[defaultSteps] = 8000
        }.named("insert-medium-activity-type")

        this add UserActivityLevelModel.insert {
            it[name] = "High"
            it[caloriesCoefficient] = 1.75F
            it[waterCoefficient] = 0.05F
            it[defaultSteps] = 10000
        }.named("insert-high-activity-type")

        // User
        this add UserModel.insert {
            it[name] = "User"
            it[email] = "login@test.test"
            it[password] = CryptoUtil.hash("password")
            it[waterTarget] = 1.2
            it[caloriesTarget] = 4.1
            it[height] = 185.0F
            it[weight] = 73.0F
            it[activityLevel] = 1
            it[goalType] = 1
            it[isMale] = true
            it[age] = 25
            it[nickname] = "nickname"
            it[stepsTarget] = 6000
        }.named("insert-base-user")


    }

    val kodein = DI {
        bindSingleton { PubSubService(AppConf.redis.db, it) }
    }

    runBlocking {
        coroutineScope {
            val handler = object : RedisSubscriber() {
                override fun onMessage(redisEvent: RedisEvent?, channel: String, message: String) {
                    if (redisEvent == null) {
                        throw Exception("Unknown event in $channel ($message)")
                    }

                    val tryDecode = redisEvent.parseData<QueryDto>(silent = true) ?: redisEvent.parseData<BatchQuery>()

                    if (tryDecode == null) return

                    launch {
                        when (tryDecode) {
                            is BatchQuery -> {
                                TransactionManager.databaseActor.send(
                                    TransactionManager.QueryEvent.BatchQueryEvent(
                                        tryDecode,
                                        redisEvent
                                    )
                                )
                            }

                            is QueryDto -> {
                                TransactionManager.databaseActor.send(
                                    TransactionManager.QueryEvent.SingleQueryEvent(
                                        tryDecode,
                                        redisEvent
                                    )
                                )
                            }
                        }
                    }
                }
            }

            withContext(Dispatchers.IO) {
                launch {
                    val pubSubService: PubSubService by kodein.instance()

                    while (true) {
                        val executionResult = TransactionManager.processedChannel.tryReceive().getOrNull() ?: continue
                        val event = executionResult.first
                        val queryResult = executionResult.second

                        val sender = event.from

                        pubSubService.publish(
                            event.mutateData(queryResult, queryResult.tid ?: event.tid),
                            sender
                        )
                    }
                }

                subscribe(handler, true, AppConf.redis.db)
            }
        }
    }
}
