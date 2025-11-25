package no.nav.emottak.state.model.ediadapter

import kotlinx.serialization.Serializable

@Serializable
data class PostMessageRequest(
    val businessDocument: String,
    val contentType: String,
    val contentTransferEncoding: String,
    val ebXmlOverrides: EbXmlInfo,
    val receiverHerIdsSubset: List<Int>? = null
)
