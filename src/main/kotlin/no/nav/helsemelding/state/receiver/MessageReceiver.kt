package no.nav.helsemelding.state.receiver

import io.github.nomisRev.kafka.receiver.KafkaReceiver
import io.github.nomisRev.kafka.receiver.ReceiverRecord
import io.github.nomisRev.kafka.receiver.ReceiverSettings
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import no.nav.helsemelding.state.config
import no.nav.helsemelding.state.model.DialogMessage
import org.apache.kafka.common.serialization.ByteArrayDeserializer
import org.apache.kafka.common.serialization.StringDeserializer
import kotlin.uuid.Uuid

private val log = KotlinLogging.logger {}

class MessageReceiver(
    private val kafkaReceiver: KafkaReceiver<String, ByteArray>
) {
    private val kafka = config().kafka.topics

    fun receiveMessages(): Flow<DialogMessage> = kafkaReceiver
        .receive(kafka.dialogMessageOut)
        .filter { record -> isValidRecordKey(record.key()) }
        .map(::toMessage)

    private suspend fun toMessage(record: ReceiverRecord<String, ByteArray>): DialogMessage {
        return DialogMessage(
            Uuid.parse(record.key()),
            record.value()
        )
    }
}

internal fun isValidRecordKey(key: String?): Boolean {
    if (key == null) {
        log.error { "Receiver record key is null. Key should be a valid uuid." }
        return false
    }

    return try {
        Uuid.parse(key)
        true
    } catch (e: IllegalArgumentException) {
        log.error { "Receiver record key is invalid and therefore ignored: $e." }
        false
    }
}

fun fakeMessageReceiver(): MessageReceiver = MessageReceiver(
    KafkaReceiver(
        ReceiverSettings(
            bootstrapServers = "",
            keyDeserializer = StringDeserializer(),
            valueDeserializer = ByteArrayDeserializer(),
            groupId = ""
        )
    )
)
