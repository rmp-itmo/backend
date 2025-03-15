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

    TableRegister.register(DbType.PGSQL, UserModel, UserLoginModel)
    TableRegister.register(DbType.PGSQL, DishTypeModel, DishModel)
    TableRegister.register(DbType.PGSQL, CacheModel, CacheToDishModel)
    TableRegister.register(DbType.PGSQL, DietDishLogModel, DietWaterLogModel)
    TableRegister.register(DbType.PGSQL, UserMenuModel, UserMenuItem)

    TransactionManager.initTables(
        forceRecreate = true,
        excludedFromRecreation = mutableSetOf(DishModel, DishTypeModel)
    ) {
        this add UserModel.insert {
            it[name] = "User"
            it[login] = "login"
            it[password] = CryptoUtil.hash("password")
        }.named("insert")
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
