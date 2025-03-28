package com.rmp.user.actions.update.calories

import com.rmp.lib.utils.redis.fsm.Fsm
import com.rmp.user.services.UserService
import org.kodein.di.DI
import org.kodein.di.instance

class UserUpdateCurrentCaloriesFsm(di: DI) : Fsm("update-calories", di) {
    private val userService: UserService by instance()

    enum class UserUpdateCaloriesState {
        UPDATE_CALORIES
    }
    override fun Fsm.registerStates() {
        on(UserUpdateCaloriesState.UPDATE_CALORIES) {
            userService.updateCalories(this)
        }
    }
}