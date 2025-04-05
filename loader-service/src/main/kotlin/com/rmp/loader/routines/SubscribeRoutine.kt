package com.rmp.loader.routines

import com.rmp.loader.core.ApiClient
import com.rmp.loader.core.Routine
import com.rmp.loader.dto.feed.FeedDto
import com.rmp.loader.dto.feed.SubscribeDto
import io.ktor.client.call.*
import io.ktor.client.request.*

object SubscribeRoutine {
    val routine = Routine {
        extend(MainPageRoutine.routine)

        addDelay(100)

        addStep("social/feed", ApiClient.Method.GET) {
            setProcessor { response ->
                state = response.body<FeedDto>()
            }
        }

        addDelay(100)

        addStep("social/user", ApiClient.Method.PATCH) {
            setBuilder {
                setBody(
                    SubscribeDto (
                        1
                    )
                )
            }
        }
    }
}