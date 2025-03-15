package com.rmp.diet.actions.target

import com.rmp.diet.services.DietTargetCheckService
import com.rmp.lib.utils.redis.fsm.Fsm
import org.kodein.di.DI
import org.kodein.di.instance

class DailyTargetCheckFsm(di: DI): Fsm("user-daily-target-check", di) {
    private val dietTargetCheckService: DietTargetCheckService by instance()

    override fun Fsm.registerStates() {
        on(DailyTargetCheckEventState.INIT) {
            dietTargetCheckService.init(this)
        }
        on(DailyTargetCheckEventState.CHECK_DISH) {
            dietTargetCheckService.checkDishes(this)
        }
        on(DailyTargetCheckEventState.CHECK_WATER) {
            dietTargetCheckService.checkWater(this)
        }
    }
}


// Автомат имеет вид:
//    +------------------+
//    | selectTargets()  |
//    +------------------+
//    |
//    v
//    +--------------------+
//    | selectDailyDishes() |
//    +--------------------+
//    |
//    +-------------------+------------------------------+
//    |                                                  |
//    "caloriesTarget != null"                         "caloriesTarget == null"
//    |                                                  |
//    v                                                  |
//    +------------------+                               |
//    | selectCalories() |                               |
//    +------------------+                               |
//    |                                                  |
//    |                                                  |
//    v                                                  |
//    +------------------+                               |
//    |  checkDishes()   |                               |
//    +------------------+                               |
//    |                                                  |
//    v                                                  |
//    +---------------------+  <-------------------------+
//    | selectDailyWater()  |
//    +---------------------+
//    |                     |
//    |"waterTarget != null"|
//    |                     |
//    |                     |
//    |                     |
//    |                     |  "waterTarget == null"
//    |                     |
//    |                     |
//    v                     |
//    +-----------------+   |
//    |  checkWater()   |   |
//    +-----------------+   |
//    |                     /
//    |                    /
//    v                   /
//    +--------------+   /
//    |  checked()   |  <
//    +--------------+