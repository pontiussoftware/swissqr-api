package ch.pontius.swissqr.api.basics

import ch.pontius.swissqr.api.extensions.errorResponse
import ch.pontius.swissqr.api.model.service.status.ErrorStatusException
import io.javalin.core.security.Role
import io.javalin.http.Context


interface RestHandler {
    val route: String
}

interface AccessManagedRestHandler {
    val permittedRoles: Set<Role>
}

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