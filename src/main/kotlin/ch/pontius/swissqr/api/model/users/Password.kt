package ch.pontius.swissqr.api.model.users

import org.mindrot.jbcrypt.BCrypt

/**
 * A [Password] as used by the user management system of Swiss QR code.
 *
 * @author Ralph Gasser
 * @version 1.0
 *
 * @see User
 */
sealed class Password(open val password: String) {
    data class PlainPassword(override val password: String): Password(password) {
        fun hash(): HashedPassword = HashedPassword(BCrypt.hashpw(this.password, BCrypt.gensalt()))
    }
    data class HashedPassword(override val password: String): Password(password)
}