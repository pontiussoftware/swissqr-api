package ch.pontius.swissqr.api.model.users

import io.javalin.core.security.Role

/**
 * [Role] a user can assume within this service.
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
enum class Role: Role {
    GENERATE, /* This role has the ability to generate QR codes. */
    SCAN /* This role has the ability to scan QR codes. */
}