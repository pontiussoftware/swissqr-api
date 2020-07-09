package ch.pontius.swissqr.api.model

import io.javalin.core.security.Role

/**
 * Role a user can assume within this service.
 *
 * @author Ralph Gasser
 * @version 1.0
 */
enum class Role: Role { GUEST, USER, ADMIN }