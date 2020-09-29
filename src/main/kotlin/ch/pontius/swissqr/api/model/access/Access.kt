package ch.pontius.swissqr.api.model.access

/**
 * Represents an [Access] to the Swiss QR code API.
 *
 * @author Ralph Gasser
 * @version 1.0
 */
data class Access(val tokenId: String, val type: AccessType) {
    val timestamp: Long = System.currentTimeMillis()
}