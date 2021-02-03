package ch.pontius.swissqr.api.basics

import ch.pontius.swissqr.api.db.MapStore
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
 * @version 1.0.0
 */
class AccessManager(val tokenStore: MapStore<Token>): AccessManager {

    companion object {
        const val TOKEN_ATTRIBUTE = "token"
    }

    override fun manage(handler: Handler, ctx: Context, permittedRoles: Set<Role>) {
        /* If given Handler has no access control, handle request and return. */
        if (permittedRoles.isEmpty()) {
            handler.handle(ctx)
        } else {
            /* Tries to obtain the access token. */
            val tokenId = ctx.header(Header.AUTHORIZATION)?.replace("Bearer ", "") ?: ctx.queryParam(TOKEN_ATTRIBUTE)
            if (tokenId == null) {
                ctx.status(401)
                return
            }

            /* Lookup the access token in the store. */
            val token = this.tokenStore[TokenId(tokenId)]
            if (token == null || !token.active) {
                ctx.status(403)
                return
            }

            /* Set token attribute to context (used for logging). */
            ctx.attribute(TOKEN_ATTRIBUTE, token)

            if (token.roles.any { permittedRoles.contains(it) }) {
                handler.handle(ctx)
            } else {
                ctx.status(403)
            }
        }
    }
}