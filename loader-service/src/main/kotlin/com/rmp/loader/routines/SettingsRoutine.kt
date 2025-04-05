package com.rmp.loader.routines

import com.rmp.loader.TODAY
import com.rmp.loader.core.ApiClient
import com.rmp.loader.core.Routine
import com.rmp.loader.dto.user.UserUpdateDto
import io.ktor.client.request.*

object SettingsRoutine {

    private val activityTypes = mapOf(
        1 to "Low",
        2 to "Medium",
        3 to "High",
    )

    private val goalTypes = mapOf(
        1 to "Lose",
        2 to "Maintain",
        3 to "Gain",
    )

    val routine = Routine {
        extend(MainPageRoutine.routine)

        addDelay(100)

        addStep("users/update", ApiClient.Method.POST) {
            setBuilder {
                setBody(UserUpdateDto(
                    date = TODAY,
                    height = randomFloat(160f, 190f),
                    weight = randomFloat(50f, 90f),
                    isMale = randomBoolean(),
                    age = randomInt(18, 52),
                    name = randomString(5),
                    email = randomString(15),
                    password = randomString(6),
                    activityType = activityTypes[randomInt(1, 3)]!!,
                    goalType = goalTypes[randomInt(1, 3)]!!,
                    nickname = randomString(7),
                ))
            }
        }
    }
}