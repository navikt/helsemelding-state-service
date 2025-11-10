package no.nav.emottak.state.config

import com.sksamuel.hoplite.Masked
import kotlinx.serialization.Serializable
import no.nav.emottak.utils.config.Server
import java.util.Properties

data class Config(
    val server: Server,
    val database: Database
)

data class Database(
    val url: Url,
    val minimumIdleConnections: MinimumIdleConnections,
    val maxLifetimeConnections: MaxLifeTimeConnections,
    val maxConnectionPoolSize: MaxConnectionPoolSize,
    val connectionTimeout: ConnectionTimeout,
    val idleConnectionTimeout: IdleConnectionTimeout,
    val driverClassName: DriverClassName,
    val mountPath: MountPath,
    val username: UserName,
    val password: Masked,
    val migrationsPath: MigrationsPath,
    val flyway: Flyway
) {
    @JvmInline
    value class Url(val value: String)

    @JvmInline
    value class MinimumIdleConnections(val value: Int)

    @JvmInline
    value class MaxLifeTimeConnections(val value: Int)

    @JvmInline
    value class MaxConnectionPoolSize(val value: Int)

    @JvmInline
    value class ConnectionTimeout(val value: Int)

    @JvmInline
    value class IdleConnectionTimeout(val value: Int)

    @JvmInline
    value class DriverClassName(val value: String)

    @JvmInline
    value class MountPath(val value: String)

    @JvmInline
    value class MigrationsPath(val value: String)

    @JvmInline
    value class UserName(val value: String)

    @Serializable
    data class Flyway(val locations: String, val baselineOnMigrate: Boolean)
}

fun Database.toProperties() = Properties()
    .apply {
        put("jdbcUrl", url.value)
        put("username", username.value)
        put("password", password.value)
        put("driverClassName", driverClassName.value)
        put("minimumIdle", minimumIdleConnections.value)
        put("maxLifetime", maxLifetimeConnections.value)
        put("maximumPoolSize", maxConnectionPoolSize.value)
        put("connectionTimeout", connectionTimeout.value)
        put("idleTimeout", idleConnectionTimeout.value)
    }
