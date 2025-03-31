package com.rmp.api.modules.forum

import com.rmp.api.utils.kodein.KodeinController
import com.rmp.lib.shared.conf.AppConf
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.routing.*
import org.kodein.di.DI

class ForumController(override val di: DI) : KodeinController() {
    override fun Route.registerRoutes() {
        route("social") {
            authenticate("default") {
                route("user") {
                    get {
                        call.process("fetch-myself", AppConf.redis.forum)
                    }
                    post("view") {
                        call.process("fetch-profile", AppConf.redis.forum)
                    }
                    patch {
                        call.process("subscription", AppConf.redis.forum)
                    }
                }
                get("feed") {
                    call.process("fetch-feed", AppConf.redis.forum)
                }
                route("post") {
                    post {
                        call.process("create-post", AppConf.redis.forum)
                    }
                    patch("upvote") {
                        call.process("upvote-post", AppConf.redis.forum)
                    }
                }
                route("share") {
                    post("dish") {
                        call.process("share-dish", AppConf.redis.forum)
                    }

                    post("achievement") {
                        call.process("share-achievement", AppConf.redis.forum)
                    }
                }
            }
        }
    }
}