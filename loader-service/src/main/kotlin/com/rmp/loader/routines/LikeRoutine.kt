package com.rmp.loader.routines

import com.rmp.loader.TODAY
import com.rmp.loader.core.ApiClient
import com.rmp.loader.core.Routine
import com.rmp.loader.dto.FetchHistoryDto
import com.rmp.loader.dto.feed.FeedDto
import com.rmp.loader.dto.feed.PostCreateDto
import com.rmp.loader.dto.feed.UpvoteDto
import com.rmp.loader.dto.user.ProfileDto
import io.ktor.client.call.*
import io.ktor.client.request.*

object LikeRoutine {
    val routine = Routine {
        startBot()

        addStep("users/stat/summary", ApiClient.Method.POST) {
            setBuilder {
                setBody(FetchHistoryDto(date = TODAY))
            }
        }

        addDelay(100)

        addStep("social/feed", ApiClient.Method.GET) {
            setProcessor { response ->
                state = response.body<FeedDto>()
            }
        }

        addDelay(100)

        extend(CreatePost.routine)

        addDelay(100)

        addStep("social/post/upvote", ApiClient.Method.PATCH) {
            setBuilder { bot ->
                val profileDto = bot.state as ProfileDto
                setBody(
                    UpvoteDto (
                        profileDto.posts.first().id,
                    )
                )
            }
        }
    }
}