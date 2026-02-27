package no.nav.helsemelding.outbound.service

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import no.nav.helsemelding.outbound.model.AppRecStatus
import no.nav.helsemelding.outbound.model.MessageDeliveryState
import no.nav.helsemelding.outbound.model.TransportStatus
import no.nav.helsemelding.outbound.repository.FakeMessageRepository

class MetricsServiceSpec : StringSpec({
    val messageRepository = FakeMessageRepository()

    val metricsService = TransactionalMetricsService(
        messageRepository
    )

    "countByTransportState should return correct counts for each TransportStatus" {

        // input data setup is in the FakeMessageRepository
        val result = metricsService.countByTransportState()

        result[TransportStatus.ACKNOWLEDGED] shouldBe 123
        result[TransportStatus.PENDING] shouldBe 234
    }

    "countByAppRecState should return correct counts for each AppRecStatus" {

        // input data setup is in the FakeMessageRepository
        val result = metricsService.countByAppRecState()

        result[AppRecStatus.OK] shouldBe 123
        result[AppRecStatus.REJECTED] shouldBe 234
    }

    "countByMessageDeliveryState should return correct counts for each MessageDeliveryState" {

        // input data setup is in the FakeMessageRepository
        val result = metricsService.countByMessageDeliveryState()

        result[MessageDeliveryState.COMPLETED] shouldBe 123
        result[MessageDeliveryState.REJECTED] shouldBe 234
    }
})
