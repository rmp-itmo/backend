package com.rmp.api.plugins

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import com.rmp.lib.shared.conf.AppConf.jwt
import com.rmp.lib.utils.security.jwt.JwtUtil

val jwtVerifier: JWTVerifier = JWT
    .require(Algorithm.HMAC256(jwt.secret))
    .withIssuer(jwt.domain)
    .build()


fun Application.configureSecurity() {
    authentication {
        jwt("default") {
            verifier(jwtVerifier)
            validate {
                JWTPrincipal(it.payload)
            }
        }

        jwt("refresh") {
            verifier(jwtVerifier)
            validate {
                val refresh = it.payload.claims["lastLogin"]

                if (refresh == null)
                    null
                else
                    JWTPrincipal(it.payload)
            }
        }
    }
}
