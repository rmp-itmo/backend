package com.rmp.diet

import com.rmp.diet.actions.food.FoodUploadFsm
import com.rmp.diet.actions.water.WaterUploadFsm
import com.rmp.diet.services.DietUploadService
import com.rmp.lib.shared.conf.AppConf
import com.rmp.lib.shared.modules.diet.DietFoodLogModel
import com.rmp.lib.shared.modules.diet.DietWaterLogModel
import com.rmp.lib.shared.modules.dish.DishModel
import com.rmp.lib.shared.modules.dish.DishTypeModel
import com.rmp.lib.shared.modules.paprika.CacheModel
import com.rmp.lib.shared.modules.paprika.CacheToDishModel
import com.rmp.lib.shared.modules.user.UserModel
import com.rmp.lib.utils.kodein.bindSingleton
import com.rmp.lib.utils.korm.DbType
import com.rmp.lib.utils.korm.TableRegister
import com.rmp.lib.utils.redis.PubSubService
import com.rmp.lib.utils.redis.RedisEvent
import com.rmp.lib.utils.redis.RedisSubscriber
import com.rmp.lib.utils.redis.fsm.FsmRouter
import com.rmp.lib.utils.redis.subscribe

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.kodein.di.DI
import org.kodein.di.instance

fun main() {
    val kodein = DI {
        bindSingleton { PubSubService(AppConf.redis.diet, it) }
        bindSingleton { DietUploadService(it) }
        bindSingleton {
            FsmRouter.routing(it) {
                fsm(FoodUploadFsm("user-upload-intake", it))
                fsm(WaterUploadFsm("user-upload-water", it))
            }
        }
    }

    // DB tables
    TableRegister.register(DbType.PGSQL, UserModel)
    TableRegister.register(DbType.PGSQL, CacheModel, CacheToDishModel, DishModel, DishTypeModel)
    TableRegister.register(DbType.PGSQL, DietFoodLogModel, DietWaterLogModel)

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
            }, true, AppConf.redis.diet)
        }
    }
}