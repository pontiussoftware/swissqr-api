package ch.pontius.swissqr.api.model.users

/**
 * An access [Token] for the Swiss QR code API.
 *
 * @author Ralph Gasser
 * @version 1.0
 */
data class Token(val id: String, val userId: String, val active: Boolean)