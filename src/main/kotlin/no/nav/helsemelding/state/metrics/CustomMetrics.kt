package no.nav.helsemelding.state.metrics

import io.github.oshai.kotlinlogging.KotlinLogging
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry

private val log = KotlinLogging.logger {}

interface Metrics {
    fun registerOutgoingMessageReceived()
}

class CustomMetrics(registry: MeterRegistry) : Metrics {
    override fun registerOutgoingMessageReceived() {
        messagesReceived.increment()
    }

    val messagesReceived: Counter =
        Counter.builder("helsemelding_outgoing_messages_received")
            .description("Number of messages received from Kafka")
            .register(registry)
}

class FakeMetrics() : Metrics {
    override fun registerOutgoingMessageReceived() {
        log.info { "helsemelding_outgoing_messages_received metric is registered" }
    }
}
