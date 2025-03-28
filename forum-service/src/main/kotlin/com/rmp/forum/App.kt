package com.rmp.forum

import com.rmp.forum.actions.*
import com.rmp.forum.dto.CreatePostDto
import com.rmp.forum.dto.FetchProfileDto
import com.rmp.forum.dto.UpvotePostDto
import com.rmp.forum.services.PostService
import com.rmp.forum.services.ProfileService
import com.rmp.forum.services.SubscribeService
import com.rmp.lib.shared.conf.AppConf
import com.rmp.lib.shared.modules.forum.PostModel
import com.rmp.lib.shared.modules.forum.UserSubsModel
import com.rmp.lib.shared.modules.forum.UserUpvoteModel
import com.rmp.lib.shared.modules.stat.GraphCacheModel
import com.rmp.lib.shared.modules.user.*
import com.rmp.lib.utils.kodein.bindSingleton
import com.rmp.lib.utils.korm.DbType
import com.rmp.lib.utils.korm.TableRegister
import com.rmp.lib.utils.redis.PubSubService
import com.rmp.lib.utils.redis.RedisEvent
import com.rmp.lib.utils.redis.RedisSubscriber
import com.rmp.lib.utils.redis.fsm.FsmRouter
import com.rmp.lib.utils.redis.subscribe
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.kodein.di.DI
import org.kodein.di.instance


fun main() {
    val kodein = DI {
        bindSingleton { PubSubService(AppConf.redis.forum, it) }
        bindSingleton { PostService(it) }
        bindSingleton { SubscribeService(it) }
        bindSingleton { ProfileService(it) }


        bindSingleton {
            FsmRouter.routing(AppConf.redis.forum, it) {
                fsm(CreatePostFsm(it))
                fsm(FetchFeedFsm(it))
                fsm(FetchProfileFsm(it))
                fsm(FetchMyselfFsm(it))
                fsm(SubscriptionFsm(it))
                fsm(UpvotePostFsm(it))
            }
        }
    }

    // DB tables
    TableRegister.register(DbType.PGSQL, UserModel, UserSubsModel, PostModel, UserUpvoteModel)

    val router by kodein.instance<FsmRouter>()

    runBlocking {
        coroutineScope {
            subscribe(object : RedisSubscriber() {
                override fun onMessage(redisEvent: RedisEvent?, channel: String, message: String) {
                    if (redisEvent == null) {
                        throw Exception("Unknown event in $channel ($message)")
                    }

                    launch {
                        router.process(redisEvent)
                    }
                }
            }, true, AppConf.redis.forum)
        }
    }
}