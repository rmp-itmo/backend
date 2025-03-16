package com.rmp.logger

import com.rmp.lib.shared.conf.AppConf
import com.rmp.lib.utils.kodein.bindSingleton
import com.rmp.lib.utils.korm.DbType
import com.rmp.lib.utils.korm.TableRegister
import com.rmp.lib.utils.redis.PubSubService
import com.rmp.lib.utils.redis.RedisEvent
import com.rmp.lib.utils.redis.RedisSubscriber
import com.rmp.lib.utils.redis.fsm.FsmRouter
import com.rmp.lib.utils.redis.subscribe
import com.rmp.logger.actions.LoggerFsm
import com.rmp.logger.models.LogModel
import com.rmp.logger.services.LoggerService
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.kodein.di.DI
import org.kodein.di.instance

fun main() {

    val kodein = DI {
        bindSingleton { PubSubService(AppConf.redis.auth, it) }
        bindSingleton { LoggerService(it) }

        bindSingleton {
            FsmRouter.routing(AppConf.redis.logger, it) {
                fsm(LoggerFsm("log", it))
            }
        }
    }

    TableRegister.register(DbType.CLICKHOUSE, LogModel)

    ConnectionManager.initTables()

    val router by kodein.instance<FsmRouter>()

    runBlocking {
        coroutineScope {
            subscribe(object : RedisSubscriber() {
                override fun onMessage(redisEvent: RedisEvent?, channel: String, message: String) {
                    if (redisEvent == null) {
                        throw Exception("Unknown event in $channel ($message)")
                    }

                    launch {
                        router.process(redisEvent)
                    }
                }
            }, true, AppConf.redis.logger)
        }
    }
}