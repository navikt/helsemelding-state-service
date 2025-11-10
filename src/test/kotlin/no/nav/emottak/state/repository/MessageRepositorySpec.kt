package no.nav.emottak.state.repository

import arrow.fx.coroutines.resourceScope
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import no.nav.emottak.state.container
import no.nav.emottak.state.database
import no.nav.emottak.state.model.MessageDeliveryState.NEW
import no.nav.emottak.state.model.MessageDeliveryState.PROCESSED
import no.nav.emottak.state.model.MessageType.DIALOG
import no.nav.emottak.state.shouldBeInstant
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction
import org.testcontainers.containers.PostgreSQLContainer
import kotlin.time.Clock
import kotlin.uuid.Uuid

class MessageRepositorySpec : StringSpec(
    {
        lateinit var container: PostgreSQLContainer<Nothing>

        beforeEach {
            container = container()
            container.start()
        }

        "Upsert state - no existing message" {
            resourceScope {
                val database = database(container.jdbcUrl)

                suspendTransaction(database) {
                    val messageRepository = ExposedMessageRepository(database)

                    val externalRefId = Uuid.random()
                    val now = Clock.System.now()
                    val messageState = messageRepository.upsertState(
                        DIALOG,
                        NEW,
                        externalRefId,
                        now
                    )

                    messageState.messageType shouldBe DIALOG
                    messageState.currentState shouldBe NEW
                    messageState.externalRefId shouldBe externalRefId
                    messageState.lastStateChange shouldBeInstant now
                }
            }
        }

        "Upsert state - existing message" {
            resourceScope {
                val database = database(container.jdbcUrl)

                suspendTransaction(database) {
                    val messageRepository = ExposedMessageRepository(database)

                    val externalRefId = Uuid.random()
                    val now = Clock.System.now()

                    messageRepository.upsertState(
                        DIALOG,
                        NEW,
                        externalRefId,
                        now
                    )

                    val stateChanged = Clock.System.now()
                    val messageState = messageRepository.upsertState(
                        DIALOG,
                        PROCESSED,
                        externalRefId,
                        stateChanged
                    )

                    messageState.messageType shouldBe DIALOG
                    messageState.currentState shouldBe PROCESSED
                    messageState.externalRefId shouldBe externalRefId
                    messageState.lastStateChange shouldBeInstant stateChanged
                }
            }
        }

        "Find or null - no value found" {
            resourceScope {
                val database = database(container.jdbcUrl)
                val messageRepository = ExposedMessageRepository(database)

                val referenceId = Uuid.random()
                messageRepository.findOrNull(referenceId) shouldBe null
            }
        }

        "Find or null - value found" {
            resourceScope {
                val database = database(container.jdbcUrl)

                suspendTransaction(database) {
                    val messageRepository = ExposedMessageRepository(database)

                    val referenceId = Uuid.random()
                    messageRepository.upsertState(
                        DIALOG,
                        NEW,
                        referenceId,
                        Clock.System.now()
                    )

                    val messageState = messageRepository.findOrNull(referenceId)
                    messageState?.externalRefId shouldBe referenceId
                }
            }
        }

        "Find for polling - empty list (no values stored)" {
            resourceScope {
                val database = database(container.jdbcUrl)
                val messageRepository = ExposedMessageRepository(database)

                messageRepository.findForPolling() shouldBe emptyList()
            }
        }

        "Find for polling - empty list (no NEW states stored)" {
            resourceScope {
                val database = database(container.jdbcUrl)

                suspendTransaction(database) {
                    val messageRepository = ExposedMessageRepository(database)

                    messageRepository.upsertState(
                        DIALOG,
                        PROCESSED,
                        Uuid.random(),
                        Clock.System.now()
                    )

                    messageRepository.upsertState(
                        DIALOG,
                        PROCESSED,
                        Uuid.random(),
                        Clock.System.now()
                    )

                    messageRepository.findForPolling() shouldBe emptyList()
                }
            }
        }

        "Find for polling - values found" {
            resourceScope {
                val database = database(container.jdbcUrl)

                suspendTransaction(database) {
                    val messageRepository = ExposedMessageRepository(database)

                    messageRepository.upsertState(
                        DIALOG,
                        NEW,
                        Uuid.random(),
                        Clock.System.now()
                    )

                    messageRepository.upsertState(
                        DIALOG,
                        NEW,
                        Uuid.random(),
                        Clock.System.now()
                    )

                    messageRepository.findForPolling().size shouldBe 2
                }
            }
        }

        afterEach { container.stop() }
    }
)
