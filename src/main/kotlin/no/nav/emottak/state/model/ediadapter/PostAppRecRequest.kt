package no.nav.emottak.state.model.ediadapter

import kotlinx.serialization.Serializable

@Serializable
data class PostAppRecRequest(
    val appRecStatus: AppRecStatus,
    val appRecErrorList: List<AppRecError>? = null,
    val ebXmlOverrides: EbXmlInfo? = null
)
