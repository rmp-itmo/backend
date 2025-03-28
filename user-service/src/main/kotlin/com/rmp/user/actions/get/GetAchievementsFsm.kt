package com.rmp.user.actions.get

import com.rmp.lib.utils.redis.fsm.Fsm
import com.rmp.user.services.UserService
import org.kodein.di.DI
import org.kodein.di.instance

class GetAchievementsFsm(di: DI): Fsm("get-achievements", di) {
    private val userService: UserService by instance()

    override fun Fsm.registerStates() {
        on(GetAchievementsEventState.INIT) {
            userService.getAchievements(this)
        }
    }
}