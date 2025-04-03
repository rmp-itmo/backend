package com.rmp.loader.core

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

const val baseUrl = "https://api.rmp.dudosyka.ru"
//const val baseUrl = "http://localhost:8080"

@Serializable
open class AnyResponse

@Serializable
open class ApiException(
    val code: Int = 0,
    val status: String = "",
    override val message: String = "",
): Exception()

sealed class Result<out R> {
    data class Success<out T>(val data: T) : Result<T>()
    data class Error(val exception: ApiException) : Result<Nothing>()
}

fun <T> Result<T>.successOr(fallback: T): T {
    return (this as? Result.Success<T>)?.data ?: fallback
}

fun Result<*>.isSuccess(): Boolean {
    return this is Result.Success
}

@Serializable
class UnauthorizedException: ApiException(401, "Unauthorized", "Unauthorized")
class BadResponse: ApiException(0, "Bad response", "Bad response")

private val client = HttpClient(CIO) {
    install(ContentNegotiation) {
        json(Json {
            ignoreUnknownKeys = true
        })
    }
}

@Serializable
data class TokenDto(
    val accessToken: String,
    val refreshToken: String,
)

class ApiClient {
    var authorizationData: TokenDto? = null

    enum class Method {
        GET, POST, DELETE, PATCH
    }

    suspend fun execute(method: Method, url: String, builder: HttpRequestBuilder.() -> Unit): HttpResponse =
        when (method) {
            Method.GET -> {
                client.get("$baseUrl/$url", builder)
            }
            Method.POST -> {
                client.post("$baseUrl/$url", builder)
            }
            Method.DELETE -> {
                client.delete("$baseUrl/$url", builder)
            }
            Method.PATCH -> {
                client.patch("$baseUrl/$url", builder)
            }
        }

    private fun response(resp: HttpResponse): Result<HttpResponse> =
        try {
            Result.Success(resp)
        } catch (e: Exception) {
            Result.Error(BadResponse())
        }

    suspend fun unauthorizedRequest(method: Method, url: String, specialBuilder: HttpRequestBuilder.() -> Unit): Result<HttpResponse> {
        val builder: HttpRequestBuilder.() -> Unit = {
            headers {
                set("Content-Type", "application/json")
            }
            specialBuilder()
        }
        val resp = execute(method, url, builder)
        return if (resp.status.value == 200) response(resp)
        else Result.Error(ApiException(resp.status.value))
    }

    private suspend fun refreshAndTryAgain(method: Method, url: String, specialBuilder: HttpRequestBuilder.() -> Unit): Result<HttpResponse> {
        val authorizationData = authorizationData.let {
            it ?: return Result.Error(UnauthorizedException())
        }

        val builder: HttpRequestBuilder.() -> Unit = {
            bearerAuth(authorizationData.refreshToken)
            headers {
                set("Content-Type", "application/json")
            }
        }

        val refreshed = execute(Method.POST, "auth/refresh", builder)

        if (refreshed.status.value != 200)
            return Result.Error(UnauthorizedException())

        val tokenDto = refreshed.body<TokenDto>()

        this.authorizationData = tokenDto

        val resp = execute(method, url) {
            headers {
                set("Content-Type", "application/json")
            }
            bearerAuth(tokenDto.accessToken)
            specialBuilder()
        }

        if (resp.status.value == 401) {
            return Result.Error(UnauthorizedException())
        }

        return response(resp)
    }

    suspend fun authorizedRequest(method: Method, url: String, specialBuilder: HttpRequestBuilder.() -> Unit): Result<HttpResponse> {
        val authorizationData = authorizationData.let {
            it ?: return Result.Error(UnauthorizedException())
        }

        val builder: HttpRequestBuilder.() -> Unit = {
            bearerAuth(authorizationData.accessToken)
            headers {
                set("Content-Type", "application/json")
            }
            specialBuilder()
        }
        val resp = execute(method, url, builder)
        if (resp.status.value == 401) {
            return refreshAndTryAgain(method, url, specialBuilder)
        }
        return response(resp)
    }
}