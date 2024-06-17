package ch.pontius.swissqr.api.model.service.status

import kotlinx.serialization.Serializable

/**
 * A success status as returned by the API.
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
@Serializable
data class SuccessStatus(val code: Int, val description: String?)