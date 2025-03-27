package com.rmp.diet

import com.rmp.diet.actions.dish.log.DishLogFsm
import com.rmp.diet.actions.dish.log.MenuHistoryFsm
import com.rmp.diet.actions.dish.service.create.DishServiceCreateFsm
import com.rmp.diet.actions.dish.service.get.DishServiceGetAllFsm
import com.rmp.diet.actions.target.check.DailyTargetCheckFsm
import com.rmp.diet.actions.target.set.DailyTargetSetFsm
import com.rmp.diet.actions.user.menu.get.GetUserMenuFsm
import com.rmp.diet.actions.user.menu.set.SetUserMenuFsm
import com.rmp.diet.actions.user.menu.set.AddMenuItemFsm
import com.rmp.diet.actions.user.menu.set.RemoveMenuItemFsm
import com.rmp.diet.actions.water.get.WaterGetHistoryFsm
import com.rmp.diet.actions.water.log.WaterLogFsm
import com.rmp.diet.services.*
import com.rmp.lib.shared.conf.AppConf
import com.rmp.lib.shared.modules.diet.DietDishLogModel
import com.rmp.lib.shared.modules.diet.DietWaterLogModel
import com.rmp.lib.shared.modules.dish.DishModel
import com.rmp.lib.shared.modules.dish.DishTypeModel
import com.rmp.lib.shared.modules.dish.UserMenuItem
import com.rmp.lib.shared.modules.dish.UserMenuModel
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
        bindSingleton { DietLogService(it) }
        bindSingleton { DishService(it) }
        bindSingleton { MenuService(it) }
        bindSingleton { DailyTargetService(it) }

        bindSingleton {
            FsmRouter.routing(AppConf.redis.diet, it) {
                fsm(DishLogFsm(it))
                fsm(WaterLogFsm(it))
                fsm(DailyTargetCheckFsm(it))
                fsm(DishServiceCreateFsm(it))
                fsm(DishServiceGetAllFsm(it))
                fsm(SetUserMenuFsm(it))
                fsm(AddMenuItemFsm(it))
                fsm(RemoveMenuItemFsm(it))
                fsm(MenuHistoryFsm(it))
                fsm(GetUserMenuFsm(it))
                fsm(DailyTargetSetFsm(it))
                fsm(WaterGetHistoryFsm(it))
            }
        }
    }

    // DB tables
    TableRegister.register(DbType.PGSQL, UserModel)
    TableRegister.register(DbType.PGSQL, DishModel, DishTypeModel)
    TableRegister.register(DbType.PGSQL, DietDishLogModel, DietWaterLogModel)
    TableRegister.register(DbType.PGSQL, UserMenuModel, UserMenuItem)

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