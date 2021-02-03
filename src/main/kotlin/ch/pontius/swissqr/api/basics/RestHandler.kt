package ch.pontius.swissqr.api.basics

import ch.pontius.swissqr.api.extensions.errorResponse
import ch.pontius.swissqr.api.model.service.status.ErrorStatusException
import ch.pontius.swissqr.api.model.users.Permission
import io.javalin.core.security.Role
import io.javalin.http.Context

/**
 * A handler for RESTful HTTP requests.
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
interface RestHandler {

    /** Route to this [RestHandler] */
    val route: String

    /**
     * Set of [Permission]s to use this [RestHandler].
     *
     * Returning an empty set here means, that no special [Permission] is required.
     */
    val requiredPermissions: Set<Permission>
}

/**
 * A [RestHandler] for HTTP GET requests.
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
interface GetRestHandler : RestHandler {
    fun get(ctx: Context) {
        try {
            doGet(ctx)
        } catch (e: ErrorStatusException) {
            ctx.errorResponse(e)
        } catch (e: Exception) {
            ctx.errorResponse(500,e.message ?: "")
        }
    }

    fun doGet(ctx: Context)
}

/**
 * A [RestHandler] for HTTP POST requests.
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
interface PostRestHandler : RestHandler {
    fun post(ctx: Context) {
        try {
            doPost(ctx)
        } catch (e: ErrorStatusException) {
            ctx.errorResponse(e)
        } catch (e: Exception) {
            ctx.errorResponse(500,e.message ?: "")
        }
    }

    fun doPost(ctx: Context)
}

/**
 * A [RestHandler] for HTTP PATCH requests.
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
interface PatchRestHandler: RestHandler {

    fun patch(ctx: Context) {
        try {
            doPatch(ctx)
        } catch (e: ErrorStatusException) {
            ctx.errorResponse(e)
        } catch (e: Exception) {
            ctx.errorResponse(500, e.message ?: "")
        }
    }

    fun doPatch(ctx: Context)

}

/**
 * A [RestHandler] for HTTP DELETE requests.
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
interface DeleteRestHandler : RestHandler {

    fun delete(ctx: Context) {
        try {
            doDelete(ctx)
        } catch (e: ErrorStatusException) {
            ctx.errorResponse(e)
        } catch (e: Exception) {
            ctx.errorResponse(500, e.message ?: "")
        }
    }

    fun doDelete(ctx: Context)
}