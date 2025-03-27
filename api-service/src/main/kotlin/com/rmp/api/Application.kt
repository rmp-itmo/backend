package com.rmp.api

import com.rmp.api.conf.ServerConf
import com.rmp.api.modules.user.UserController
import com.rmp.api.modules.auth.AuthController
import com.rmp.api.modules.diet.ServiceController
import com.rmp.api.modules.paprika.PaprikaController
import com.rmp.api.utils.api.ApiService
import com.rmp.api.plugins.*
import com.rmp.api.utils.kodein.bindSingleton
import com.rmp.api.utils.kodein.regKodein
import com.rmp.lib.shared.conf.AppConf
import com.rmp.lib.utils.redis.RedisEvent
import com.rmp.lib.utils.redis.RedisSubscriber
import com.rmp.lib.utils.redis.subscribe
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import org.kodein.di.DI
import org.kodein.di.instance

val kodein = DI {
    // ----- Services ------
    bindSingleton { ApiService(it) }

    // ----- Controllers ------
    bindSingleton { AuthController(it) }
    bindSingleton { UserController(it) }
    bindSingleton { PaprikaController(it) }
    bindSingleton { ServiceController(it) }
}

fun main() {
    embeddedServer(Netty, port = ServerConf.port, host = ServerConf.host, module = Application::module)
        .start()

    val apiService: ApiService by kodein.instance<ApiService>()

    runBlocking {
        coroutineScope {
            subscribe(object : RedisSubscriber() {
                override fun onMessage(redisEvent: RedisEvent?, channel: String, message: String) {
                    if (redisEvent == null) {
                        throw Exception("Unknown event in $channel ($message)")
                    }

                    apiService.receive(redisEvent)
                }
            }, true, AppConf.redis.api)
        }
    }
}

fun Application.module() {
    configureSecurity()
    configureMonitoring()
    configureSerialization()
    configureExceptionFilter()

    regKodein("/", kodein)
}