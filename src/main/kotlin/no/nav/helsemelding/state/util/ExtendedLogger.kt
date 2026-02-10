package no.nav.helsemelding.state.util

import io.github.oshai.kotlinlogging.KLogger
import io.opentelemetry.api.trace.Span
import io.opentelemetry.instrumentation.annotations.WithSpan
import org.slf4j.MDC

class ExtendedLogger(private val logger: KLogger) {

    @WithSpan("extendedlogger.debug")
    fun debug(msg: () -> Any?) {
        val ctx = Span.current().spanContext
        MDC.putCloseable("trace_id", ctx.traceId).use {
            logger.debug(msg)
        }
    }

    @WithSpan("extendedlogger.info")
    fun info(msg: () -> Any?) {
        val ctx = Span.current().spanContext
        MDC.putCloseable("trace_id", ctx.traceId).use {
            logger.info(msg)
        }
    }

    @WithSpan("extendedlogger.warn")
    fun warn(msg: () -> Any?) {
        val ctx = Span.current().spanContext
        MDC.putCloseable("trace_id", ctx.traceId).use {
            logger.warn(msg)
        }
    }

    @WithSpan("extendedlogger.error")
    fun error(msg: () -> Any?) {
        val ctx = Span.current().spanContext
        MDC.putCloseable("trace_id", ctx.traceId).use {
            logger.error(msg)
        }
    }

    @WithSpan("extendedlogger.error")
    fun error(err: Throwable?, msg: () -> Any?) {
        val ctx = Span.current().spanContext
        MDC.putCloseable("trace_id", ctx.traceId).use {
            logger.error(err) { msg }
        }
    }
}
