package ch.pontius.`swissqr-api`.extensions

import ch.pontius.`swissqr-api`.model.ErrorStatusException
import ch.pontius.`swissqr-api`.model.Status
import io.javalin.http.Context

fun Context.errorResponse(status: Int, errorMessage: String) {
    this.status(status)
    this.json(Status.ErrorStatus(status, errorMessage))
}

fun Context.errorResponse(error: ErrorStatusException) {
    this.status(error.statusCode)
    this.json(Status.ErrorStatus(error.statusCode, error.message))
}