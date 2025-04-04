package com.rmp.loader.routines

import com.rmp.loader.core.ApiClient
import com.rmp.loader.core.Routine
import com.rmp.loader.dto.LoginDto
import com.rmp.loader.dto.SignupDto
import io.ktor.client.request.*

object HelloRoutine {
    val routine: Routine = Routine {
        addStep("users/create", ApiClient.Method.POST, false) {
            setBuilder { bot ->
                val login = LoginDto("${randomString(6)}-${System.currentTimeMillis()}", "password")
                bot.state = login
                val signupDto = SignupDto(
                        name = "test-${randomString(6)}-${System.currentTimeMillis()}",
                        email = login.login,
                        password = login.password,
                        height = randomFloat(1f, 2f),
                        weight = randomFloat(45f, 150f),
                        activityType = randomLong(1, 3),
                        goalType = randomLong(1, 3),
                        isMale = randomBoolean(),
                        age = randomInt(18, 70),
                        registrationDate = randomInt(1, 100)
                    )
                setBody(signupDto)
            }
        }

        addDelay(100)

        authorize {
            state as LoginDto
        }
    }
}