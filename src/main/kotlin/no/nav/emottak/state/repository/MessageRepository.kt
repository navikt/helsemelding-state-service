package no.nav.emottak.state.repository

import no.nav.emottak.state.model.MessageDeliveryState
import no.nav.emottak.state.model.MessageDeliveryState.NEW
import no.nav.emottak.state.model.MessageState
import no.nav.emottak.state.model.MessageType
import no.nav.emottak.state.repository.Messages.currentState
import no.nav.emottak.state.util.ExposedUuidTransformer
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.datetime.CurrentTimestamp
import org.jetbrains.exposed.v1.datetime.timestamp
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction
import org.jetbrains.exposed.v1.jdbc.upsertReturning
import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.uuid.Uuid

object Messages : Table("messages") {
    val id = uuid("id").transform(ExposedUuidTransformer())

    override val primaryKey = PrimaryKey(id)

    val externalRefId = uuid("external_reference_id")
        .transform(ExposedUuidTransformer())
        .uniqueIndex()

    val messageType = enumerationByName("message_type", 100, MessageType::class)
    val currentState = enumerationByName("current_state", 100, MessageDeliveryState::class)
    val lastStateChange = timestamp("last_state_change")
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")
}

interface MessageRepository {
    suspend fun upsertState(
        messageType: MessageType,
        state: MessageDeliveryState,
        externalRefId: Uuid,
        lastStateChange: Instant
    ): MessageState

    suspend fun findOrNull(id: Uuid): MessageState?

    suspend fun findForPolling(limit: Int = 100): List<MessageState>
}

class ExposedMessageRepository(private val database: Database) : MessageRepository {
    override suspend fun upsertState(
        messageType: MessageType,
        state: MessageDeliveryState,
        externalRefId: Uuid,
        lastStateChange: Instant
    ): MessageState = Messages.upsertReturning(
        keys = arrayOf(Messages.externalRefId),
        onUpdate = { update ->
            update[currentState] = state
            update[Messages.lastStateChange] = lastStateChange
            update[Messages.updatedAt] = CurrentTimestamp
        }
    ) { insert ->
        insert[Messages.messageType] = messageType
        insert[Messages.externalRefId] = externalRefId
        insert[currentState] = state
        insert[Messages.lastStateChange] = lastStateChange
    }
        .single()
        .toMessageState()

    override suspend fun findOrNull(id: Uuid): MessageState? = suspendTransaction(database) {
        Messages
            .selectAll().where { Messages.externalRefId eq id }
            .singleOrNull()
            ?.toMessageState()
    }

    override suspend fun findForPolling(limit: Int): List<MessageState> = suspendTransaction(database) {
        Messages
            .selectAll().where { currentState eq NEW }
            .map { it.toMessageState() }
    }

    private fun ResultRow.toMessageState(): MessageState = MessageState(
        this[Messages.id],
        this[Messages.messageType],
        this[Messages.externalRefId],
        this[currentState],
        this[Messages.lastStateChange],
        this[Messages.createdAt],
        this[Messages.updatedAt]
    )
}

class FakeMessageRepository : MessageRepository {
    private val messages = HashMap<Uuid, MessageState>()

    override suspend fun upsertState(
        messageType: MessageType,
        state: MessageDeliveryState,
        externalRefId: Uuid,
        lastStateChange: Instant
    ): MessageState {
        val now = Clock.System.now()
        val existing = messages.values.find { it.externalRefId == externalRefId }

        val updated = if (existing != null) {
            existing.copy(
                currentState = state,
                lastStateChange = lastStateChange,
                updatedAt = now
            )
        } else {
            val newId = Uuid.random()

            MessageState(
                id = newId,
                messageType = messageType,
                currentState = state,
                externalRefId = externalRefId,
                lastStateChange = lastStateChange,
                createdAt = now,
                updatedAt = now
            )
        }

        messages[updated.id] = updated
        return updated
    }

    override suspend fun findOrNull(id: Uuid): MessageState? = messages[id]

    override suspend fun findForPolling(limit: Int): List<MessageState> = messages
        .values
        .filter { it.currentState == NEW }
        .take(limit)
}
