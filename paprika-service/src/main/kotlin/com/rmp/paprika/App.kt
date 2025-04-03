package com.rmp.paprika

import com.rmp.lib.shared.conf.AppConf
import com.rmp.lib.shared.modules.dish.DishModel
import com.rmp.lib.shared.modules.dish.DishTypeModel
import com.rmp.lib.shared.modules.paprika.CacheModel
import com.rmp.lib.shared.modules.paprika.CacheToDishModel
import com.rmp.lib.utils.kodein.bindSingleton
import com.rmp.lib.utils.korm.DbType
import com.rmp.lib.utils.korm.TableRegister
import com.rmp.lib.utils.metrics.MetricsProvider
import com.rmp.lib.utils.redis.PubSubService
import com.rmp.lib.utils.redis.RedisEvent
import com.rmp.lib.utils.redis.RedisSubscriber
import com.rmp.lib.utils.redis.fsm.FsmRouter
import com.rmp.lib.utils.redis.subscribe
import com.rmp.paprika.actions.cache.UpdateCacheFsm
import com.rmp.paprika.actions.meal.GenerateMealFsm
import com.rmp.paprika.actions.menu.GenerateMenuFsm
import com.rmp.paprika.services.CacheService
import com.rmp.paprika.services.DishService
import com.rmp.paprika.services.PaprikaService
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import kotlinx.coroutines.*
import org.kodein.di.DI
import org.kodein.di.instance

val prometheusMeterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)

fun main() {
    val kodein = DI {
        bindSingleton { PubSubService(AppConf.redis.paprika, prometheusMeterRegistry, it) }
        bindSingleton { CacheService(it) }
        bindSingleton { DishService(it) }
        bindSingleton { PaprikaService(it) }

        bindSingleton {
            FsmRouter.routing(AppConf.redis.paprika, it) {
                fsm(GenerateMealFsm(it))
                fsm(GenerateMenuFsm(it))
                fsm(UpdateCacheFsm(it))
            }
        }
    }

    TableRegister.register(DbType.PGSQL, CacheModel, CacheToDishModel, DishModel, DishTypeModel)

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

            subscribe(handler, true, AppConf.redis.paprika)
        }
    }
}