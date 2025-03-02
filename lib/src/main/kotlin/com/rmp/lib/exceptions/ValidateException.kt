package com.rmp.lib.exceptions

import com.rmp.lib.utils.serialization.Json
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
class ValidateException internal constructor(): BaseException(400, "Bad request") {
    private val errorList: ErrorList = ErrorList()
    @Serializable
    private data class ErrorList(
        val validateErrors: MutableList<ValidateError> = mutableListOf()
    )
    @Serializable
    data class ValidateError(
        val field: String,
        val error: String
    )

    @Transient
    private val json = Json.serializer

    companion object {
        fun build(builder: ValidateException.() -> Unit): ValidateException =
            ValidateException().apply(builder).apply {
                data = json.encodeToString(ErrorList.serializer(), errorList)
            }
    }

    fun addError(validateError: ValidateError) {
        errorList.validateErrors.add(validateError)
    }
}