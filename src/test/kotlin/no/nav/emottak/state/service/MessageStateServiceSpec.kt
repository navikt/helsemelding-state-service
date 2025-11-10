package no.nav.emottak.state.service

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import no.nav.emottak.state.model.MessageDeliveryState.COMPLETED
import no.nav.emottak.state.model.MessageDeliveryState.NEW
import no.nav.emottak.state.model.MessageType.DIALOG
import no.nav.emottak.state.repository.FakeMessageRepository
import no.nav.emottak.state.repository.FakeMessageStateHistoryRepository
import no.nav.emottak.state.repository.FakeMessageStateTransactionRepository
import kotlin.time.Clock
import kotlin.time.DurationUnit.MILLISECONDS
import kotlin.time.toDuration
import kotlin.uuid.Uuid

class MessageStateServiceSpec : StringSpec(
    {
        "Create initial state - creates state and one history entry" {
            val messageStateService = transactionalMessageStateService()

            val externalRefId = Uuid.random()
            val now = Clock.System.now()

            val snapshot = messageStateService.createInitialState(
                messageType = DIALOG,
                externalRefId = externalRefId,
                initialState = NEW,
                occurredAt = now
            )

            snapshot.messageState.externalRefId shouldBe externalRefId
            snapshot.messageState.currentState shouldBe NEW
            snapshot.messageStateChange.size shouldBe 1
            snapshot.messageStateChange.first().newState shouldBe NEW
            snapshot.messageStateChange.first().oldState shouldBe null
        }

        "Record state change - updates current state and appends history" {
            val messageStateService = transactionalMessageStateService()

            val externalRefId = Uuid.random()
            val now1 = Clock.System.now()
            val now2 = now1.plus(10L.toDuration(MILLISECONDS))

            messageStateService.createInitialState(
                DIALOG,
                externalRefId,
                NEW,
                now1
            )
            val updated = messageStateService.recordStateChange(
                DIALOG,
                NEW,
                COMPLETED,
                externalRefId,
                now2
            )

            updated.messageState.currentState shouldBe COMPLETED
            updated.messageStateChange.size shouldBe 2
            updated.messageStateChange.last().newState shouldBe COMPLETED
        }

        "Get message state  - returns null when missing" {
            val messageStateService = transactionalMessageStateService()
            messageStateService.getMessageState(Uuid.random()).shouldBeNull()
        }

        "Find messages for polling - returns only NEW messages" {
            val messageStateService = transactionalMessageStateService()

            val externalRefId1 = Uuid.random()
            val externalRefId2 = Uuid.random()
            val now = Clock.System.now()

            messageStateService.createInitialState(
                DIALOG,
                externalRefId1,
                NEW,
                now
            )
            messageStateService.createInitialState(
                DIALOG,
                externalRefId2,
                COMPLETED,
                now
            )

            val result = messageStateService.findMessagesForPolling(10)

            result.size shouldBe 1
            result.first().externalRefId shouldBe externalRefId1
        }
    }
)

private fun transactionalMessageStateService(): TransactionalMessageStateService {
    val messageRepository = FakeMessageRepository()
    val historyRepository = FakeMessageStateHistoryRepository()
    val txRepository = FakeMessageStateTransactionRepository(
        messageRepository,
        historyRepository
    )

    return TransactionalMessageStateService(
        messageRepository,
        historyRepository,
        txRepository
    )
}
