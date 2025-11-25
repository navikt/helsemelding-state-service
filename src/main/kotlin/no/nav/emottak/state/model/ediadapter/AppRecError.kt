package no.nav.emottak.state.model.ediadapter

import kotlinx.serialization.Serializable

@Serializable
data class AppRecError(
    val errorCode: String? = null,
    val details: String? = null,
    val description: String? = null,
    val oid: String? = null
)
