package no.nav.emottak.state.model

import kotlin.time.Instant
import kotlin.uuid.Uuid

data class MessageState(
    val id: Uuid,
    val messageType: MessageType,
    val externalRefId: Uuid,
    val currentState: MessageDeliveryState,
    val lastStateChange: Instant,
    val createdAt: Instant,
    val updatedAt: Instant
)
