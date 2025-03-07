package com.rmp.paprika

import com.rmp.lib.shared.conf.AppConf
import com.rmp.lib.shared.modules.dish.DishModel
import com.rmp.lib.shared.modules.dish.DishTypeModel
import com.rmp.lib.shared.modules.paprika.CacheModel
import com.rmp.lib.shared.modules.paprika.CacheToDishModel
import com.rmp.lib.utils.kodein.bindSingleton
import com.rmp.lib.utils.korm.DbType
import com.rmp.lib.utils.korm.TableRegister
import com.rmp.lib.utils.redis.PubSubService
import com.rmp.lib.utils.redis.RedisEvent
import com.rmp.lib.utils.redis.RedisSubscriber
import com.rmp.lib.utils.redis.fsm.FsmRouter
import com.rmp.lib.utils.redis.subscribe
import com.rmp.paprika.services.CacheService
import com.rmp.paprika.services.DishService
import com.rmp.paprika.services.PaprikaService
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.kodein.di.DI
import org.kodein.di.instance

fun main(args: Array<String>) {
    val kodein = DI {
        bindSingleton { PubSubService(AppConf.redis.auth, it) }
        bindSingleton { CacheService(it) }
        bindSingleton { DishService(it) }
        bindSingleton { PaprikaService(it) }

        bindSingleton {
            FsmRouter.routing(it) {

            }
        }
    }

    TableRegister.register(DbType.PGSQL, CacheModel, CacheToDishModel, DishModel, DishTypeModel)

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
            }, true, AppConf.redis.auth)
        }
    }
}