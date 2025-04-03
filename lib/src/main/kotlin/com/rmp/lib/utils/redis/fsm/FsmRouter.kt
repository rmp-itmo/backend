package com.rmp.lib.utils.redis.fsm

import com.rmp.lib.exceptions.BaseException
import com.rmp.lib.exceptions.InternalServerException
import com.rmp.lib.shared.conf.AppConf
import com.rmp.lib.utils.log.Logger
import com.rmp.lib.utils.redis.PubSubService
import com.rmp.lib.utils.redis.RedisEvent
import com.rmp.lib.utils.redis.SerializableClass
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.instance
import kotlin.reflect.KClass
import kotlin.reflect.full.superclasses
import kotlin.uuid.ExperimentalUuidApi

typealias ExceptionHandler = suspend RedisEvent.(exception: Exception) -> Unit

class FsmRouter(override val di: DI) : DIAware {
    private val fsmService: FsmService = object : FsmService(di) {}
    private val routes: MutableMap<String, Fsm> = mutableMapOf()
    private val pubSubService: PubSubService by instance()
    val exceptionHandlers: MutableMap<KClass<*>, ExceptionHandler> = mutableMapOf()

    companion object {
        fun routing(serviceName: String, di: DI, configure: FsmRouter.() -> Unit): FsmRouter =
            FsmRouter(di).apply(configure).apply {
                handle<BaseException> { exception ->
                    Logger.debugException("${exception::class.simpleName} caught!", exception, serviceName)
                    fsmService.apply {
                        switchOnApi(exception)
                    }
                }

                if (Exception::class !in exceptionHandlers) {
                    handle<Exception> { exception ->
                        Logger.debugException("${exception::class.qualifiedName} caught!", exception, serviceName)
                        fsmService.apply {
                            switchOnApi(InternalServerException(exception.message ?: "Internal server error"))
                        }
                    }
                }
            }
    }

    suspend fun applyToFsm(f: suspend FsmService.() -> Unit) {
        f.invoke(fsmService)
    }

    suspend inline fun <reified T: SerializableClass> RedisEvent.respond(data: T) {
        applyToFsm {
            switchOnApi(data)
        }
    }

    suspend inline fun RedisEvent.respondService(to: String, crossinline f: (event: RedisEvent) -> RedisEvent) {
        applyToFsm {
            f(this@respondService).switchOn(to)
        }
    }

    fun fsm(eventType: String, configure: Fsm.() -> Unit) {
        routes += Pair(eventType, object: Fsm(eventType, di) {
            override fun Fsm.registerStates() {
                apply(configure)
            }
        })
    }

    fun fsm(fsm: Fsm) {
        routes += Pair(fsm.event, fsm.apply { registerStates() })
    }

    @Suppress("UNCHECKED_CAST")
    inline fun <reified T: Exception> handle(noinline processor: suspend RedisEvent.(exception: T) -> Unit) {
        exceptionHandlers += T::class to processor as ExceptionHandler
    }

    @OptIn(ExperimentalUuidApi::class)
    suspend fun process(event: RedisEvent) {
        try {
            val fsm = routes[event.eventType] ?: throw Exception("Unknown event type: ${event.eventType}")
            if (event.from == AppConf.redis.db && event.dbRequest != null) {
                pubSubService.processDbResponse(event)
                return
            }
            fsm.process(event)
        } catch (e: Exception) {
            fsmService.transaction(event) {
                rollback()
            }

            if (e::class in exceptionHandlers) {
                exceptionHandlers[e::class]?.invoke(event, e)
                return
            }

            val superTypeHandler = e::class.superclasses.firstOrNull { it in exceptionHandlers }

            if (superTypeHandler != null) {
                exceptionHandlers[superTypeHandler]?.invoke(event, e)
            } else {
                exceptionHandlers[Exception::class]?.invoke(event, e)
            }
        }
    }
}