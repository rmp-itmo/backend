package com.rmp.lib.utils.redis.fsm

import com.rmp.lib.utils.redis.RedisEvent
import org.kodein.di.DI
import org.kodein.di.DIAware

abstract class Fsm(val event: String, override val di: DI): DIAware {
    private val states: MutableMap<String, suspend RedisEvent.() -> Unit> = mutableMapOf()
    private var any: (suspend RedisEvent.() -> Unit)? = null

    fun on(state: Enum<*>, processor: suspend RedisEvent.() -> Unit) {
        states += Pair(state.name, processor)
    }

    fun on(state: String, processor: suspend RedisEvent.() -> Unit) {
        states += Pair(state, processor)
    }

    fun any(processor: suspend RedisEvent.() -> Unit) {
        any = processor
    }

    suspend fun process(event: RedisEvent) {
        val stateProcessor = states[event.eventState.state]
            ?: any ?: throw Exception("Unknown event state ${event.eventState.state}, available states is ${states.keys}")

        event.apply {
            stateProcessor(this)
        }
    }

    abstract fun Fsm.registerStates()
}