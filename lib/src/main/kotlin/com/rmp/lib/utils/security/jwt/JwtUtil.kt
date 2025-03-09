package com.rmp.lib.utils.security.jwt

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.interfaces.Payload
import com.rmp.lib.exceptions.ForbiddenException
import com.rmp.lib.shared.conf.AppConf
import com.rmp.lib.shared.modules.auth.dto.AuthorizedUser
import com.rmp.lib.utils.log.Logger
import java.util.*

object JwtUtil {
    fun createToken(userId: Long, lastLogin: Long? = null): String {
        return JWT.create()
            .withIssuer(AppConf.jwt.domain)
            .withIssuedAt(Date(System.currentTimeMillis()))
            .withExpiresAt(
                Date(
                    System.currentTimeMillis() +
                            (if (lastLogin != null) AppConf.jwt.refreshExpirationTime else AppConf.jwt.expirationTime) * 1000
                )
            )
            .apply {
                withClaim("id", userId)
                if (lastLogin != null)
                    withClaim("lastLogin", lastLogin)

            }.sign(Algorithm.HMAC256(AppConf.jwt.secret))
    }

    fun decodeAccessToken(principal: Payload): AuthorizedUser = AuthorizedUser(
        id = principal.getClaim("id")!!.asLong(),
        lastLogin = principal.getClaim("lastLogin").let {
            if (it.isNull) null
            else it.asLong()
        }
    )

    fun verifyNative(token: String): AuthorizedUser {
        Logger.debug("Verify native", "main")
        val jwtVerifier = JWT
            .require(Algorithm.HMAC256(AppConf.jwt.secret))
            .withIssuer(AppConf.jwt.domain)
            .build()

        val verified = jwtVerifier.verify(token)
        return if (verified != null) {
            val claims = verified.claims
            val currentTime: Long = System.currentTimeMillis() / 1000
            if (currentTime > (claims["exp"]?.asInt()
                    ?: 0) || claims["iss"]?.asString() != AppConf.jwt.domain
            ) {
                Logger.debug("expired exception", "main")
                throw ForbiddenException()
            }
            else {
                AuthorizedUser(
                    id = claims["id"]?.asLong() ?: throw ForbiddenException(),
                )
            }
        } else {
            Logger.debug("verified exception", "main")
            throw ForbiddenException()
        }
    }

}