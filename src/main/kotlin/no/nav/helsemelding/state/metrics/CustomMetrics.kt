package no.nav.helsemelding.state.metrics

import io.github.oshai.kotlinlogging.KotlinLogging
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry

private val log = KotlinLogging.logger {}

interface Metrics {
    fun registerOutgoingMessageReceived()
    fun registerOutgoingMessageFailed(errorType: String)
}

class CustomMetrics(val registry: MeterRegistry) : Metrics {
    override fun registerOutgoingMessageReceived() {
        Counter.builder("helsemelding_outgoing_messages_received")
            .description("Number of outgoing messages received from Kafka")
            .register(registry)
            .increment()
    }

    override fun registerOutgoingMessageFailed(errorType: String) {
        Counter.builder("helsemelding_outgoing_messages_failed")
            .description("Number of outgoing messages that failed to be processed")
            .tag("errorType", errorType)
            .register(registry)
            .increment()
    }
}

class FakeMetrics() : Metrics {
    override fun registerOutgoingMessageReceived() {
        log.info { "helsemelding_outgoing_messages_received metric is registered" }
    }

    override fun registerOutgoingMessageFailed(errorType: String) {
        log.info { "helsemelding_outgoing_messages_failed metric is registered with errorType: $errorType" }
    }
}
