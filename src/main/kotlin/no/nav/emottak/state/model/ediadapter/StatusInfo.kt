package no.nav.emottak.state.model.ediadapter

import kotlinx.serialization.Serializable

@Serializable
data class StatusInfo(
    val receiverHerId: Int,
    val transportDeliveryState: DeliveryState,
    val appRecStatus: AppRecStatus? = null
)
