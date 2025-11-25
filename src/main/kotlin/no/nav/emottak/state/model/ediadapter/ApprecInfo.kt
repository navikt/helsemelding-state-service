package no.nav.emottak.state.model.ediadapter

import kotlinx.serialization.Serializable

@Serializable
data class ApprecInfo(
    val receiverHerId: Int,
    val appRecStatus: AppRecStatus? = null,
    val appRecErrorList: List<AppRecError>? = null
)
