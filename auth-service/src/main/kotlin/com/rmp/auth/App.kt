package com.rmp.auth

import com.rmp.auth.actions.auth.AuthFsm
import com.rmp.auth.actions.getme.GetMeFsm
import com.rmp.auth.actions.refresh.RefreshFsm
import com.rmp.auth.services.AuthService
import com.rmp.auth.services.GetMeService
import com.rmp.auth.services.RefreshService
import com.rmp.lib.shared.conf.AppConf
import com.rmp.lib.shared.modules.user.UserLoginModel
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
        bindSingleton { PubSubService(AppConf.redis.auth, it) }
        bindSingleton { AuthService(it) }
        bindSingleton { RefreshService(it) }
        bindSingleton { GetMeService(it) }

        bindSingleton {
            FsmRouter.routing(it) {
                fsm(AuthFsm(it))
                fsm(RefreshFsm(it))
                fsm(GetMeFsm(it))
            }
        }
    }

    TableRegister.register(DbType.PGSQL, UserModel, UserLoginModel)

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