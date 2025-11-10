package no.nav.emottak.state.service

import no.nav.emottak.state.model.MessageDeliveryState
import no.nav.emottak.state.model.MessageState
import no.nav.emottak.state.model.MessageStateSnapshot
import no.nav.emottak.state.model.MessageType
import no.nav.emottak.state.repository.MessageRepository
import no.nav.emottak.state.repository.MessageStateHistoryRepository
import no.nav.emottak.state.repository.MessageStateTransactionRepository
import kotlin.time.Instant
import kotlin.uuid.Uuid

interface MessageStateService {
    /**
     * Stores a newly accepted message from the adapter. Creates the initial state and
     * corresponding history entry atomically.
     */
    suspend fun createInitialState(
        messageType: MessageType,
        externalRefId: Uuid,
        initialState: MessageDeliveryState,
        occurredAt: Instant
    ): MessageStateSnapshot

    /**
     * Updates an existing message if a state transition occurred. Returns the updated snapshot.
     */
    suspend fun recordStateChange(
        messageType: MessageType,
        oldState: MessageDeliveryState?,
        newState: MessageDeliveryState,
        externalRefId: Uuid,
        occurredAt: Instant
    ): MessageStateSnapshot

    /**
     * Retrieves a message's current state + full history. Useful for debugging, APIs, or observability.
     */
    suspend fun getMessageState(messageId: Uuid): MessageStateSnapshot?

    /**
     * Returns messages in states eligible for polling (e.g., SENT, DELIVERED…)
     * Used by the poller to discover which messages need state verification.
     */
    suspend fun findMessagesForPolling(limit: Int = 100): List<MessageState>
}

class TransactionalMessageStateService(
    private val messageRepository: MessageRepository,
    private val historyRepository: MessageStateHistoryRepository,
    private val transactionRepository: MessageStateTransactionRepository
) : MessageStateService {
    override suspend fun createInitialState(
        messageType: MessageType,
        externalRefId: Uuid,
        initialState: MessageDeliveryState,
        occurredAt: Instant
    ): MessageStateSnapshot = transactionRepository
        .recordStateChange(
            messageType = messageType,
            oldState = null,
            newState = initialState,
            externalRefId = externalRefId,
            occurredAt = occurredAt
        )

    override suspend fun recordStateChange(
        messageType: MessageType,
        oldState: MessageDeliveryState?,
        newState: MessageDeliveryState,
        externalRefId: Uuid,
        occurredAt: Instant
    ): MessageStateSnapshot = transactionRepository
        .recordStateChange(
            messageType = messageType,
            newState = newState,
            externalRefId = externalRefId,
            occurredAt = occurredAt,
            oldState = oldState
        )

    override suspend fun getMessageState(messageId: Uuid): MessageStateSnapshot? {
        val state = messageRepository.findOrNull(messageId) ?: return null
        val history = historyRepository.findAll(messageId)

        return MessageStateSnapshot(state, history)
    }

    override suspend fun findMessagesForPolling(limit: Int): List<MessageState> =
        messageRepository.findForPolling(limit)
}
