package com.rmp.loader.routines

import com.rmp.loader.core.ApiClient
import com.rmp.loader.core.Routine
import com.rmp.loader.dto.achievement.AchievementsDto
import com.rmp.loader.dto.achievement.ShareAchievementDto
import io.ktor.client.call.*
import io.ktor.client.request.*

object AchievementRoutine {
    val routine = Routine {
        extend(MainPageRoutine.routine)

        addDelay(100)

        addStep("users/stat", ApiClient.Method.GET) {
            setProcessor { response ->
                state = response.body<AchievementsDto>()
            }
        }

        addDelay(100)

        addStep("social/share/achievement", ApiClient.Method.POST) {
            setBuilder {
                setBody(ShareAchievementDto(
                    achievementType = 3,
                    current = 52,
                    percentage = 72
                ))
            }
        }
    }
}