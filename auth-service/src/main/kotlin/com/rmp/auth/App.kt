package com.rmp.auth

import com.rmp.auth.actions.auth.AuthFsm
import com.rmp.auth.actions.getme.GetMeFsm
import com.rmp.auth.actions.refresh.RefreshFsm
import com.rmp.auth.services.AuthService
import com.rmp.auth.services.GetMeService
import com.rmp.auth.services.RefreshService
import com.rmp.lib.shared.conf.AppConf
import com.rmp.lib.shared.modules.user.UserGoalTypeModel
import com.rmp.lib.shared.modules.user.UserLoginModel
import com.rmp.lib.shared.modules.user.UserModel
import com.rmp.lib.utils.kodein.bindSingleton
import com.rmp.lib.utils.korm.DbType
import com.rmp.lib.utils.korm.TableRegister
import com.rmp.lib.utils.metrics.MetricsProvider
import com.rmp.lib.utils.redis.PubSubService
import com.rmp.lib.utils.redis.RedisEvent
import com.rmp.lib.utils.redis.RedisSubscriber
import com.rmp.lib.utils.redis.fsm.FsmRouter
import com.rmp.lib.utils.redis.subscribe
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import kotlinx.coroutines.*
import org.kodein.di.DI
import org.kodein.di.instance

val prometheusMeterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)

fun main() {
    val kodein = DI {
        bindSingleton { PubSubService(AppConf.redis.auth, prometheusMeterRegistry, it) }
        bindSingleton { AuthService(it) }
        bindSingleton { RefreshService(it) }
        bindSingleton { GetMeService(it) }

        bindSingleton {
            FsmRouter.routing(AppConf.redis.auth, it) {
                fsm(AuthFsm(it))
                fsm(RefreshFsm(it))
                fsm(GetMeFsm(it))

/* -------------------- Exception handler usage example -------------------- */

/*
                handle<Exception> { exception ->
                    Logger.debugException("Exception caught!", exception, "auth")

                    // Use respond to send response to ApiService
                    respond(InternalServerException("Internal server error!"))

                    // Use respond service to event to other services
                    respondService(AppConf.redis.serviceName) { event ->
                        event.mutateState(...).mutateData(...)
                    }
                }
*/
            }
        }
    }

    TableRegister.register(DbType.PGSQL, UserModel, UserLoginModel, UserGoalTypeModel)

    val router by kodein.instance<FsmRouter>()

    runBlocking {
        coroutineScope {
            val handler = object : RedisSubscriber() {
                override fun onMessage(redisEvent: RedisEvent?, channel: String, message: String) {
                    if (redisEvent == null) {
                        throw Exception("Unknown event in $channel ($message)")
                    }

                    launch(context = Dispatchers.IO.limitedParallelism(Int.MAX_VALUE)) {
                        router.process(redisEvent)
                    }
                }
            }

            withContext(Dispatchers.IO) {
                launch {
                    MetricsProvider.start(prometheusMeterRegistry)
                }
            }

            subscribe(handler, true, AppConf.redis.auth)
        }
    }
}