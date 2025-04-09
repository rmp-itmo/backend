package com.rmp.user

import com.rmp.lib.shared.conf.AppConf
import com.rmp.lib.shared.modules.dish.DishModel
import com.rmp.lib.shared.modules.dish.UserMenuItem
import com.rmp.lib.shared.modules.training.TrainingIntensityModel
import com.rmp.lib.shared.modules.training.TrainingTypeModel
import com.rmp.lib.shared.modules.training.UserTrainingLogModel
import com.rmp.lib.shared.modules.user.*
import com.rmp.lib.utils.kodein.bindSingleton
import com.rmp.lib.utils.korm.DbType
import com.rmp.lib.utils.korm.TableRegister
import com.rmp.lib.utils.metrics.MetricsProvider
import com.rmp.lib.utils.redis.PubSubService
import com.rmp.lib.utils.redis.RedisEvent
import com.rmp.lib.utils.redis.RedisSubscriber
import com.rmp.lib.utils.redis.fsm.FsmRouter
import com.rmp.lib.utils.redis.subscribe
import com.rmp.user.actions.create.UserCreateFsm
import com.rmp.user.actions.get.GetAchievementsFsm
import com.rmp.user.actions.get.UserGetFsm
import com.rmp.user.actions.heart.HeartLogFsm
import com.rmp.user.actions.sleep.fetch.FetchSleepHistoryFsm
import com.rmp.user.actions.sleep.set.SetSleepFsm
import com.rmp.user.actions.update.calories.UserUpdateCurrentCaloriesFsm
import com.rmp.user.actions.steps.log.StepsLogFsm
import com.rmp.user.actions.steps.update.UserStepsUpdateFsm
import com.rmp.user.actions.update.UserUpdateFsm
import com.rmp.user.actions.summary.UserSummaryFsm
import com.rmp.user.actions.target.UserTargetCheckFsm
import com.rmp.user.actions.training.get.TrainingIntensiveGetFsm
import com.rmp.user.actions.training.get.TrainingTypeGetFsm
import com.rmp.user.actions.training.get.TrainingsGetFsm
import com.rmp.user.actions.training.log.TrainingLogFsm
import com.rmp.user.services.*
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import kotlinx.coroutines.*
import org.kodein.di.DI
import org.kodein.di.instance
import io.micrometer.prometheusmetrics.PrometheusConfig

val prometheusMeterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)

fun main() {
    val kodein = DI {
        bindSingleton { PubSubService(AppConf.redis.user, prometheusMeterRegistry, it) }
        bindSingleton { UserService(it) }
        bindSingleton { SleepService(it) }
        bindSingleton { StepsService(it) }
        bindSingleton { HeartService(it) }
        bindSingleton { TrainingService(it) }


        bindSingleton {
            FsmRouter.routing(AppConf.redis.user, it) {
                fsm(UserUpdateCurrentCaloriesFsm(it))
                fsm(UserCreateFsm(it))
                fsm(UserGetFsm(it))
                fsm(UserUpdateFsm(it))
                fsm(UserStepsUpdateFsm(it))
                fsm(SetSleepFsm(it))
                fsm(FetchSleepHistoryFsm(it))

                fsm(StepsLogFsm(it))
                fsm(HeartLogFsm(it))

                fsm(GetAchievementsFsm(it))
                fsm(UserSummaryFsm(it))
                fsm(UserTargetCheckFsm(it))

                // Trainings
                fsm(TrainingLogFsm(it))
                fsm(TrainingTypeGetFsm(it))
                fsm(TrainingIntensiveGetFsm(it))
                fsm(TrainingsGetFsm(it))
            }
        }
    }

    // DB tables
    TableRegister.register(DbType.PGSQL,
        UserModel, UserGoalTypeModel,
        UserActivityLevelModel, UserSleepModel,
        UserHeartLogModel, UserStepsLogModel,
        UserAchievementsModel, TrainingTypeModel, TrainingIntensityModel,
        UserTrainingLogModel, UserMenuItem, DishModel
    )

    val router by kodein.instance<FsmRouter>()
    val pub by kodein.instance<PubSubService>()

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

            subscribe(handler, true, AppConf.redis.user)
        }
    }
}