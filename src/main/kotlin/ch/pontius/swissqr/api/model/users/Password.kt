package ch.pontius.swissqr.api.model.users

import org.mindrot.jbcrypt.BCrypt

/**
 * A [Password] as used by the user management system of Swiss QR code.
 *
 * @author Ralph Gasser
 * @version 1.0.0
 *
 * @see User
 */
sealed class Password(open val value: String) {
    data class PlainPassword(override val value: String): Password(value) {
        fun hash(): HashedPassword = HashedPassword(BCrypt.hashpw(this.value, BCrypt.gensalt()))
    }
    data class HashedPassword(override val value: String): Password(value)
}