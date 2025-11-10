package no.nav.emottak.state.repository

import arrow.fx.coroutines.resourceScope
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldStartWith
import io.kotest.matchers.types.shouldBeInstanceOf
import no.nav.emottak.state.container
import no.nav.emottak.state.database
import no.nav.emottak.state.model.MessageDeliveryState.COMPLETED
import no.nav.emottak.state.model.MessageDeliveryState.NEW
import no.nav.emottak.state.model.MessageDeliveryState.PROCESSED
import no.nav.emottak.state.model.MessageType.DIALOG
import org.jetbrains.exposed.v1.exceptions.ExposedSQLException
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction
import org.postgresql.util.PSQLException
import org.testcontainers.containers.PostgreSQLContainer
import kotlin.time.Clock
import kotlin.uuid.Uuid

class MessageStateHistoryRepositorySpec : StringSpec(
    {
        lateinit var container: PostgreSQLContainer<Nothing>

        beforeEach {
            container = container()
            container.start()
        }

        "Append message state history - no message reference" {
            resourceScope {
                val database = database(container.jdbcUrl)

                suspendTransaction(database) {
                    val messageStateHistoryRepository = ExposedMessageStateHistoryRepository(database)

                    val exception = shouldThrow<ExposedSQLException> {
                        messageStateHistoryRepository.append(
                            Uuid.random(),
                            null,
                            PROCESSED,
                            Clock.System.now()
                        )
                    }

                    exception.cause.shouldBeInstanceOf<PSQLException>()
                    exception.cause?.message shouldStartWith ("ERROR: insert or update on table")
                }
            }
        }

        "Append message state history - empty history" {
            resourceScope {
                val database = database(container.jdbcUrl)

                suspendTransaction(database) {
                    val messageStateRepository = ExposedMessageRepository(database)
                    val messageStateHistoryRepository = ExposedMessageStateHistoryRepository(database)

                    val externalRefId = Uuid.random()
                    val state = NEW
                    val stateChanged = Clock.System.now()

                    messageStateRepository.upsertState(
                        DIALOG,
                        state,
                        externalRefId,
                        stateChanged
                    )

                    val messageStateChanges = messageStateHistoryRepository.append(
                        externalRefId,
                        null,
                        state,
                        stateChanged
                    )

                    messageStateChanges.size shouldBe 1
                    val messageStateChange = messageStateChanges.first()

                    messageStateChange.messageId shouldBe externalRefId
                    messageStateChange.oldState shouldBe null
                    messageStateChange.newState shouldBe state
                }
            }
        }

        "Append message state history - existing history" {
            resourceScope {
                val database = database(container.jdbcUrl)

                suspendTransaction(database) {
                    val messageStateRepository = ExposedMessageRepository(database)
                    val messageStateHistoryRepository = ExposedMessageStateHistoryRepository(database)

                    val externalRefId = Uuid.random()
                    val state = NEW
                    val stateChanged = Clock.System.now()

                    messageStateRepository.upsertState(
                        DIALOG,
                        state,
                        externalRefId,
                        stateChanged
                    )

                    messageStateHistoryRepository.append(
                        externalRefId,
                        null,
                        state,
                        stateChanged
                    )

                    val messageStateChanges = messageStateHistoryRepository.append(
                        externalRefId,
                        NEW,
                        PROCESSED,
                        Clock.System.now()
                    )

                    messageStateChanges.size shouldBe 2

                    val messageStateChange = messageStateChanges.last()

                    messageStateChange.messageId shouldBe externalRefId
                    messageStateChange.oldState shouldBe NEW
                    messageStateChange.newState shouldBe PROCESSED
                }
            }
        }

        "Find all state changes - empty history" {
            resourceScope {
                val database = database(container.jdbcUrl)
                val messageStateHistoryRepository = ExposedMessageStateHistoryRepository(database)

                val id = Uuid.random()
                val messageStateChanges = messageStateHistoryRepository.findAll(id)

                messageStateChanges.size shouldBe 0
            }
        }

        "Find all state changes - history exists" {
            resourceScope {
                val database = database(container.jdbcUrl)

                suspendTransaction(database) {
                    val messageStateRepository = ExposedMessageRepository(database)
                    val messageStateHistoryRepository = ExposedMessageStateHistoryRepository(database)

                    val externalRefId = Uuid.random()
                    val first = messageStateRepository.upsertState(
                        DIALOG,
                        NEW,
                        externalRefId,
                        Clock.System.now()
                    )

                    messageStateHistoryRepository.append(
                        externalRefId,
                        null,
                        first.currentState,
                        first.lastStateChange
                    )

                    val second = messageStateRepository.upsertState(
                        DIALOG,
                        PROCESSED,
                        externalRefId,
                        Clock.System.now()
                    )

                    messageStateHistoryRepository.append(
                        externalRefId,
                        PROCESSED,
                        second.currentState,
                        second.lastStateChange
                    )

                    val third = messageStateRepository.upsertState(
                        DIALOG,
                        COMPLETED,
                        externalRefId,
                        Clock.System.now()
                    )

                    messageStateHistoryRepository.append(
                        externalRefId,
                        PROCESSED,
                        third.currentState,
                        third.lastStateChange
                    )

                    messageStateHistoryRepository.findAll(externalRefId).size shouldBe 3
                }
            }
        }
    }
)
