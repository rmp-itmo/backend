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
                        patch {
                            call.process("user-update-menu", AppConf.redis.diet)
                        }
                        delete {
                            call.process("remove-menu-item", AppConf.redis.diet)
                        }
                        get {
                            call.process("get-user-menu", AppConf.redis.diet)
                        }
                    }

                    route("stat") {
                        get {
                            call.process("get-achievements", AppConf.redis.user)
                        }
                        post("water") {
                            call.process("water-history", AppConf.redis.diet)
                        }
                        post("menu") {
                            call.process("menu-history", AppConf.redis.diet)
                        }
                        post("summary") {
                            call.process("user-summary", AppConf.redis.user)
                        }
                    }

                    route("log") {
                        post("water") {
                           call.process("user-upload-water-log", AppConf.redis.diet)
                        }

                        post("dish") {
                           call.process("user-upload-dish-log", AppConf.redis.diet)
                        }

                        post("steps") {
                            call.process("user-upload-steps-log", AppConf.redis.user)
                        }

                        post("heart") {
                            call.process("user-upload-heart-log", AppConf.redis.user)
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