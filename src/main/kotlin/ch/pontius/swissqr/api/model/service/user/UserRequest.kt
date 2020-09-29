package ch.pontius.swissqr.api.model.service.user

import ch.pontius.swissqr.api.model.users.Password

/**
 *
 * @author Ralph Gasser
 * @version 1.0
 */
data class UserRequest(val email: String, val password: Password.PlainPassword)