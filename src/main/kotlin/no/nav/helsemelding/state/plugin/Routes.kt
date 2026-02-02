package no.nav.helsemelding.state.plugin

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.micrometer.prometheus.PrometheusMeterRegistry
import io.opentelemetry.api.trace.Span
import io.opentelemetry.instrumentation.annotations.SpanAttribute
import io.opentelemetry.instrumentation.annotations.WithSpan
import no.nav.helsemelding.payloadsigning.client.HttpPayloadSigningClient
import no.nav.helsemelding.payloadsigning.client.scopedAuthHttpClient
import no.nav.helsemelding.payloadsigning.model.Direction
import no.nav.helsemelding.payloadsigning.model.PayloadRequest
import no.nav.helsemelding.state.model.MessageDeliveryState
import no.nav.helsemelding.state.service.MessageStateService
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import kotlin.uuid.Uuid

val log = LoggerFactory.getLogger("no.nav.helsemelding.state.App")

fun Application.configureRoutes(
    registry: PrometheusMeterRegistry,
    messageStateService: MessageStateService
) {
    routing { internalRoutes(registry, messageStateService) }
}

fun Route.internalRoutes(
    registry: PrometheusMeterRegistry,
    messageStateService: MessageStateService
) {
    get("/prometheus") {
        call.respond(registry.scrape())
    }
    route("/internal") {
        get("/health/liveness") {
            call.respondText("I'm alive! :)")
        }
        get("/health/readiness") {
            call.respondText("I'm ready! :)")
        }
    }

    get("/telemetry-test") {
        val scope = "api://dev-gcp.helsemelding.payload-signing-service/.default"
        val payloadSigningServiceUrl = "https://helsemelding-payload-signing-service.intern.dev.nav.no"

        val scopedClient = scopedAuthHttpClient(scope)
        val payloadSigningClient = HttpPayloadSigningClient(scopedClient, payloadSigningServiceUrl)

        // 1
        // Request to another service
        val payloadRequestOut = PayloadRequest(
            direction = Direction.OUT,
            bytes = "<MsgHead><Body>hello world</Body></MsgHead>".toByteArray()
        )

        try {
            val response = payloadSigningClient.signPayload(payloadRequestOut)
            log.info("Signing test: 1 Response from signing service: Left: ${response.leftOrNull()} Right: ${response.getOrNull()}")
            log.info("Signing test: 1 test succeeded signPayload(OUT)")
        } catch (e: Exception) {
            log.error("Signing test: 1 Exception occurred while calling signing service", e)
            log.info("Signing test: 1 test failed signPayload(OUT)")
        }

        // 2
        // Request to database
        val messageSnapshot = messageStateService.getMessageSnapshot(Uuid.random())

        // 3
        // Logg a value
        Span.current().setAttribute("message.log.key1", "value1")

        // 4
        // Call a method
        val newMessageState = updateMessageState(Uuid.random(), MessageDeliveryState.COMPLETED)

        // 5
        // Throw exception
        try {
            unsafeOperation()
        } catch (e: Exception) {
            // Just go on
        }

        // Get trace_id og span_id
        val ctx = Span.current().spanContext
        val traceId = ctx.traceId
        val spanId = ctx.spanId

        MDC.putCloseable("trace_id", ctx.traceId).use {
            MDC.putCloseable("span_id", ctx.spanId).use {
                log.info("Detailed log her. TraceId: $traceId SpanId: $spanId")
            }
        }

        call.respond(HttpStatusCode.OK)
    }
}

@WithSpan("message.state.update")
fun updateMessageState(
    @SpanAttribute("message.id") messageId: Uuid,
    @SpanAttribute("message.state.value") state: MessageDeliveryState
): String {
    Span.current().setAttribute("message.log.key2", "value2")

    return "New state for message $messageId is $state"
}

@WithSpan("message.state.unsafe.operation")
fun unsafeOperation() {
    throw IllegalStateException("This is a test exception for telemetry")
}
