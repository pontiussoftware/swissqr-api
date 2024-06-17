package ch.pontius.swissqr.api.model.service.status

import kotlinx.serialization.Serializable

/**
 * An error status as returned by the API.
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
@Serializable
data class ErrorStatus(val code : Int, val description: String?) {
    init {
        check(code != 200) { "Error status cannot have status code $code." }
    }
}