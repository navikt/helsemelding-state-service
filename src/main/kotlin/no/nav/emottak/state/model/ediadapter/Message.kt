package no.nav.emottak.state.model.ediadapter

import kotlinx.serialization.Serializable
import no.nav.emottak.utils.serialization.InstantSerializer
import no.nav.emottak.utils.serialization.UuidSerializer
import java.time.Instant
import kotlin.uuid.Uuid

@Serializable
data class Message(
    @Serializable(with = UuidSerializer::class)
    val id: Uuid? = null,
    val contentType: String? = null,
    val receiverHerId: Int? = null,
    val senderHerId: Int? = null,
    val businessDocumentId: String? = null,
    @Serializable(with = InstantSerializer::class)
    val businessDocumentGenDate: Instant? = null,
    val isAppRec: Boolean? = null
)
