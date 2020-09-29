package ch.pontius.swissqr.api.model.users

import io.javalin.core.security.Role

/**
 * Role a user can assume within this service.
 *
 * @author Ralph Gasser
 * @version 1.0
 */
enum class Role: Role {
    GUEST, /* A guest user. Guest users are limited to 100 requests per month and can only create a single App token. */
    CLIENT, /* A client user. Client users can create as many App tokens as they like and are limited to a specified amount of requests per month. */
    CLIENT_UNLIMITED /* A unlimited client user. Client users can create as many App tokens as they like and are not limited in the number of requests per month. */
}