package com.rmp.stat

import com.rmp.lib.shared.conf.AppConf
import com.rmp.lib.shared.modules.stat.GraphCacheModel
import com.rmp.lib.shared.modules.target.TargetLogModel
import com.rmp.lib.shared.modules.user.*
import com.rmp.lib.utils.kodein.bindSingleton
import com.rmp.lib.utils.korm.DbType
import com.rmp.lib.utils.korm.TableRegister
import com.rmp.lib.utils.redis.PubSubService
import com.rmp.lib.utils.redis.RedisEvent
import com.rmp.lib.utils.redis.RedisSubscriber
import com.rmp.lib.utils.redis.fsm.FsmRouter
import com.rmp.lib.utils.redis.subscribe
import com.rmp.stat.actions.graphs.heart.HeartGraphFsm
import com.rmp.stat.actions.graphs.sleep.SleepGraphFsm
import com.rmp.stat.actions.target.TargetUpdateLogFsm
import com.rmp.stat.services.GraphService
import com.rmp.stat.services.TargetService
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.kodein.di.DI
import org.kodein.di.instance


fun main() {
    val kodein = DI {
        bindSingleton { PubSubService(AppConf.redis.stat, it) }
        bindSingleton { GraphService(it) }
        bindSingleton { TargetService(it) }


        bindSingleton {
            FsmRouter.routing(AppConf.redis.stat, it) {
                fsm(HeartGraphFsm(it))
                fsm(SleepGraphFsm(it))
                fsm(TargetUpdateLogFsm(it))
            }
        }
    }

    // DB tables
    TableRegister.register(DbType.PGSQL, UserModel, UserHeartLogModel,
        UserSleepModel, GraphCacheModel, TargetLogModel)

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
            }, true, AppConf.redis.stat)
        }
    }
}