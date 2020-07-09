package ch.pontius.swissqr.api.basics

import io.javalin.core.security.AccessManager
import io.javalin.core.security.Role
import io.javalin.http.Context
import io.javalin.http.Handler

/**
 *
 * @author Ralph Gasser
 * @version 1.0
 */
object AccessManager: AccessManager {
    override fun manage(handler: Handler, ctx: Context, permittedRoles: Set<Role>) {
        when {
            permittedRoles.isEmpty() -> handler.handle(ctx) //fallback in case no roles are set, none are required
            permittedRoles.contains(ch.pontius.swissqr.api.model.Role.GUEST) -> handler.handle(ctx)
            else -> ctx.status(401)
        }
    }
}