package no.nav.emottak.state.util

import org.jetbrains.exposed.v1.core.ColumnTransformer
import java.util.UUID
import kotlin.uuid.Uuid
import kotlin.uuid.toJavaUuid
import kotlin.uuid.toKotlinUuid

class ExposedUuidTransformer : ColumnTransformer<UUID, Uuid> {
    override fun wrap(value: UUID): Uuid = value.toKotlinUuid()

    override fun unwrap(value: Uuid): UUID = value.toJavaUuid()
}
