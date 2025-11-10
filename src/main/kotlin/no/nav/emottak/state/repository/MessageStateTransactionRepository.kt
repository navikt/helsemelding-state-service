package no.nav.emottak.state.repository

import no.nav.emottak.state.model.MessageDeliveryState
import no.nav.emottak.state.model.MessageStateSnapshot
import no.nav.emottak.state.model.MessageType
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction
import kotlin.time.Instant
import kotlin.uuid.Uuid

interface MessageStateTransactionRepository {
    suspend fun recordStateChange(
        messageType: MessageType,
        oldState: MessageDeliveryState?,
        newState: MessageDeliveryState,
        externalRefId: Uuid,
        occurredAt: Instant
    ): MessageStateSnapshot
}

class ExposedMessageStateTransactionRepository(
    private val database: Database,
    private val messageRepository: MessageRepository,
    private val historyRepository: MessageStateHistoryRepository
) : MessageStateTransactionRepository {

    override suspend fun recordStateChange(
        messageType: MessageType,
        oldState: MessageDeliveryState?,
        newState: MessageDeliveryState,
        externalRefId: Uuid,
        occurredAt: Instant
    ): MessageStateSnapshot = suspendTransaction(database) {
        val state = messageRepository.upsertState(
            messageType = messageType,
            state = newState,
            externalRefId = externalRefId,
            lastStateChange = occurredAt
        )

        val history = historyRepository.append(
            messageId = externalRefId,
            oldState = oldState,
            newState = newState,
            changedAt = occurredAt
        )

        MessageStateSnapshot(state, history)
    }
}

class FakeMessageStateTransactionRepository(
    private val messageRepo: MessageRepository,
    private val historyRepo: MessageStateHistoryRepository
) : MessageStateTransactionRepository {
    override suspend fun recordStateChange(
        messageType: MessageType,
        oldState: MessageDeliveryState?,
        newState: MessageDeliveryState,
        externalRefId: Uuid,
        occurredAt: Instant
    ): MessageStateSnapshot {
        val state = messageRepo.upsertState(
            messageType = messageType,
            state = newState,
            externalRefId = externalRefId,
            lastStateChange = occurredAt
        )

        val history = historyRepo.append(
            messageId = externalRefId,
            oldState = oldState,
            newState = newState,
            changedAt = occurredAt
        )

        return MessageStateSnapshot(state, history)
    }
}
