package ch.pontius.`swissqr-api`.model

/**
 *
 * @author Ralph Gasser
 * @version 1.0
 */
sealed class Status(val statusCode: Int, val statusDescription: String?) {
    class SuccessStatus(statusDescription: String?) : Status(200, statusDescription)

    class ErrorStatus(statusCode: Int, statusDescription: String?): Status(statusCode, statusDescription) {
        init {
            check(statusCode != 200) { "Error status cannot have status code $statusCode." }
        }
    }
}