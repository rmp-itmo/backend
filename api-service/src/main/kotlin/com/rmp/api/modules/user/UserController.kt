package com.rmp.api.modules.user

import com.rmp.api.utils.kodein.KodeinController
import com.rmp.lib.shared.conf.AppConf
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.routing.*
import org.kodein.di.DI


class UserController(override val di: DI) : KodeinController() {
    override fun Route.registerRoutes() {
            route("users") {

                post("create") {
                    call.processUnauthorized("create-user", AppConf.redis.user)
                }

                authenticate("default") {
                    get {
                        call.process("get-user", AppConf.redis.user)
                    }

                    route("update") {
                        post {
                            call.process("update-user", AppConf.redis.user)
                        }
                        post("steps") {
                            call.process("update-user-steps", AppConf.redis.user)
                        }
                    }

                    route("menu") {
                        post {
                            call.process("set-user-menu", AppConf.redis.diet)
                        }
                        get {
                            call.process("get-user-menu", AppConf.redis.diet)
                        }
                    }

                    route("day") {
                        post("water") {
                            call.process("water-get-per-day", AppConf.redis.diet)
                        }
                    }

                    route("log") {
                        post("water") {
                           call.process("user-upload-water-log", AppConf.redis.diet)
                        }

                        post("dish") {
                           call.process("user-upload-dish-log", AppConf.redis.diet)
                        }
                    }

                    route("target") {
                        post("check") {
                            call.process("user-daily-target-check", AppConf.redis.diet)
                        }
                        post("set") {
                            call.process("user-daily-target-set", AppConf.redis.diet)
                        }
                    }
                }
        }
    }
}