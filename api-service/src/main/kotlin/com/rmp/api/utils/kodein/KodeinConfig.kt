package com.rmp.api.utils.kodein

import com.rmp.lib.utils.log.Logger
import io.ktor.server.application.*
import io.ktor.server.routing.*
import org.kodein.type.jvmType
import org.kodein.di.*

inline fun <reified T : Any> DI.MainBuilder.bindSingleton(crossinline callback: (DI) -> T) {
    bind<T>() with singleton { callback(this@singleton.di) }
}

fun Application.regKodein(
    baseControllerRouting: String,
    kodein: DI
) {
    routing {
        route(baseControllerRouting) {
            for (bind in kodein.container.tree.bindings) {
                val bindClass = bind.key.type.jvmType as? Class<*>?
                if (bindClass != null && KodeinController::class.java.isAssignableFrom(bindClass)) {
                    val res by kodein.Instance(bind.key.type)
                    Logger.debug("Registering '$res' routes...", "info")
                    (res as KodeinController).apply { this@route.registerRoutes() }
                }
            }
        }
    }
}