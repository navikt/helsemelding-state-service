package no.nav.emottak.state.model.ediadapter

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ErrorMessage(
    @SerialName("Error")
    val error: String? = null,

    @SerialName("ErrorCode")
    val errorCode: Int,

    @SerialName("ValidationErrors")
    val validationErrors: List<String>? = null,

    @SerialName("StackTrace")
    val stackTrace: String? = null,

    @SerialName("RequestId")
    val requestId: String
)
