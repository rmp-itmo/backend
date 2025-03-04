package com.rmp.logger.actions

import com.rmp.lib.utils.redis.fsm.Fsm
import com.rmp.logger.services.LoggerService
import org.kodein.di.DI
import org.kodein.di.instance

class LoggerFsm(event: String, di: DI): Fsm(event, di) {
    private val loggerService: LoggerService by instance()

    override fun Fsm.registerStates() {
        any {
            loggerService.processEvent(this)
        }
    }
}