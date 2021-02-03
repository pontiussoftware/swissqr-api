package ch.pontius.swissqr.api.basics

import ch.pontius.swissqr.api.db.MapStore
import ch.pontius.swissqr.api.model.service.status.Status
import ch.pontius.swissqr.api.model.users.Token
import ch.pontius.swissqr.api.model.users.TokenId
import io.javalin.core.security.AccessManager
import io.javalin.core.security.Role
import io.javalin.core.util.Header
import io.javalin.http.Context
import io.javalin.http.Handler

/**
 * The [AccessManager] that mediates access to the API endpoints based on an authentication token.
 * The authentication token can be provided in two ways:
 *
 * Authorization header (preferred): Authorization Bearer <token>
 * GET query parameter : ?token=<token>
 *
 * @author Ralph Gasser
 * @version 1.0.1
 */
class AccessManager(private val tokenStore: MapStore<Token>): AccessManager {

    companion object {
        const val API_KEY_HEADER = "X-API-Key"
        const val API_KEY_PARAM = "api_key"
    }

    override fun manage(handler: Handler, ctx: Context, permittedRoles: Set<Role>) {
        /* If given Handler has no access control, handle request and return. */
        if (permittedRoles.isEmpty()) {
            handler.handle(ctx)
        } else {
            /* Tries to obtain the access token. */
            val tokenId = ctx.header(API_KEY_HEADER)?.replace("Bearer ", "") ?: ctx.queryParam(API_KEY_PARAM)
            if (tokenId == null) {
                ctx.status(401)
                ctx.json(Status.ErrorStatus(403, "API token is missing from request."))
                return
            }

            /* Lookup the access token in the store. */
            val token = this.tokenStore[TokenId(tokenId)]
            if (token == null) {
                ctx.status(401)
                ctx.json(Status.ErrorStatus(401, "API Token is nonexistent."))
                return
            } else {
                ctx.attribute(API_KEY_PARAM, token)
            }

            /* Check if the access token is still active. */
            if (!token.active) {
                ctx.status(403)
                ctx.json(Status.ErrorStatus(403, "API Token has been invalidated."))
                return
            }

            /* Set token attribute to context (used for logging). */
            if (permittedRoles.all { token.roles.contains(it) }) {
                handler.handle(ctx)
            } else {
                ctx.status(403)
                ctx.json(Status.ErrorStatus(403, "API Token does not provide access to given method."))
            }
        }
    }
}