package ch.pontius.swissqr.api.model.service.status


/**
 * Exception that can be expressed in terms of an HTTP status code. Used by the API.
 *
 * @author Ralph Gasser
 * @version 1.1.0
 */
data class ErrorStatusException(val status: Int, val description: String) : Exception(description)