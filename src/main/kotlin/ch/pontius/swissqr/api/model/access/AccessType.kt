package ch.pontius.swissqr.api.model.access

/**
 * Type of [Access] to Swiss QR API.
 *
 * @author Ralph Gasser
 * @version 1.0
 */
enum class AccessType {
    GENERATOR, /* Access to the QR generator service. */
    SCANNER /* Access to the QR scanner service. */
}