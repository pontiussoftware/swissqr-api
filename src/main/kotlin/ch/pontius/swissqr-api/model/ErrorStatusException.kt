package ch.pontius.`swissqr-api`.model


/**
 * Exception that can be expressed in terms of an HTTP status code. Used by
 * [RestHandler][ch.pontius.swissqr.basics.RestHandler] implementations
 *
 * @author Ralph Gasser
 * @version 1.0
 */
data class ErrorStatusException(val statusCode: Int, val status: String) : Exception(status)