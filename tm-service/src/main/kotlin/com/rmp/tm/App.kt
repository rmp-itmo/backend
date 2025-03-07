package com.rmp.tm

import com.rmp.lib.shared.conf.AppConf
import com.rmp.lib.shared.modules.dish.DishModel
import com.rmp.lib.shared.modules.dish.DishTypeModel
import com.rmp.lib.shared.modules.paprika.CacheModel
import com.rmp.lib.shared.modules.paprika.CacheToDishModel
import com.rmp.lib.shared.modules.user.UserModel
import com.rmp.lib.utils.kodein.bindSingleton
import com.rmp.lib.utils.korm.DbType
import com.rmp.lib.utils.korm.TableRegister
import com.rmp.lib.utils.korm.query.BatchQuery
import com.rmp.lib.utils.korm.query.QueryDto
import com.rmp.lib.utils.redis.PubSubService
import com.rmp.lib.utils.redis.RedisEvent
import com.rmp.lib.utils.redis.RedisSubscriber
import com.rmp.lib.utils.redis.subscribe
import com.rmp.tm.conf.ServiceConf
import com.rmp.tm.korm.TransactionManager
import kotlinx.coroutines.*
import org.kodein.di.DI
import org.kodein.di.instance

fun main(args: Array<String>) {
    TransactionManager.init {
        jdbcUrl = ServiceConf.dbConf.jdbcUrl
        driverClassName = ServiceConf.dbConf.driverClassName
        username = ServiceConf.dbConf.username
        password = ServiceConf.dbConf.password
    }

    TableRegister.register(DbType.PGSQL, UserModel)
    TableRegister.register(DbType.PGSQL, CacheModel, CacheToDishModel, DishModel, DishTypeModel)

    TransactionManager.initTables(forceRecreate = true)

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
                            event.switchData(queryResult, queryResult.tid ?: event.tid),
                            sender
                        )
                    }
                }

                subscribe(handler, true, AppConf.redis.db)
            }
        }
    }
}
