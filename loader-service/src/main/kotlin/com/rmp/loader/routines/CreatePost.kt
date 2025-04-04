package com.rmp.loader.routines

import com.rmp.loader.core.ApiClient
import com.rmp.loader.core.Routine
import com.rmp.loader.dto.feed.PostCreateDto
import com.rmp.loader.dto.user.ProfileDto
import io.ktor.client.call.*
import io.ktor.client.request.*

object CreatePost {
    val routine = Routine {
        addStep("social/post", ApiClient.Method.POST) {
            setBuilder {
                setBody(
                    PostCreateDto(
                        title = randomString(6),
                        text = randomString(15)
                    )
                )
            }
        }

        addDelay(100)

        addStep("social/user", ApiClient.Method.GET) {
            setProcessor { response ->
                state = response.body<ProfileDto>()
            }
        }

        addDelay(100)
    }
}