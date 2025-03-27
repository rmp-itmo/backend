package com.rmp.user

import com.rmp.lib.shared.conf.AppConf
import com.rmp.lib.shared.modules.user.UserActivityLevelModel
import com.rmp.lib.shared.modules.user.UserGoalTypeModel
import com.rmp.lib.shared.modules.user.UserModel
import com.rmp.lib.shared.modules.user.UserSleepModel
import com.rmp.lib.utils.kodein.bindSingleton
import com.rmp.lib.utils.korm.DbType
import com.rmp.lib.utils.korm.TableRegister
import com.rmp.lib.utils.redis.PubSubService
import com.rmp.lib.utils.redis.RedisEvent
import com.rmp.lib.utils.redis.RedisSubscriber
import com.rmp.lib.utils.redis.fsm.FsmRouter
import com.rmp.lib.utils.redis.subscribe
import com.rmp.user.actions.create.UserCreateFsm
import com.rmp.user.actions.get.UserGetFsm
import com.rmp.user.actions.sleep.fetch.FetchSleepHistoryFsm
import com.rmp.user.actions.sleep.set.SetSleepFsm
import com.rmp.user.actions.update.steps.UserStepsUpdateFsm
import com.rmp.user.actions.update.user.UserUpdateFsm
import com.rmp.user.services.SleepService
import com.rmp.user.services.UserService
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.kodein.di.DI
import org.kodein.di.instance


fun main() {
    val kodein = DI {
        bindSingleton { PubSubService(AppConf.redis.user, it) }
        bindSingleton { UserService(it) }
        bindSingleton { SleepService(it) }

        bindSingleton {
            FsmRouter.routing(AppConf.redis.user, it) {
                fsm(UserCreateFsm(it))
                fsm(UserGetFsm(it))
                fsm(UserUpdateFsm(it))
                fsm(UserStepsUpdateFsm(it))
                fsm(SetSleepFsm(it))
                fsm(FetchSleepHistoryFsm(it))
            }
        }
    }

    // DB tables
    TableRegister.register(DbType.PGSQL, UserModel, UserGoalTypeModel, UserActivityLevelModel, UserSleepModel)

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
            }, true, AppConf.redis.user)
        }
    }
}