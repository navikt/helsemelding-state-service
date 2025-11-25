package no.nav.emottak.state.model.ediadapter

import kotlinx.serialization.Serializable

@Serializable
data class GetBusinessDocumentResponse(
    val businessDocument: String,
    val contentType: String,
    val contentTransferEncoding: String
)
