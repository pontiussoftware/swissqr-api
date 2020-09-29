package ch.pontius.swissqr.api.model.users

import ch.pontius.swissqr.api.model.Entity
import java.util.*
import java.util.zip.CRC32

/**
 * A [User] of the Swiss QR code system.
 *
 * @author Ralph Gasser
 * @version 1.0
 */
data class User(
    override val id: String,
    var email: String,
    var password: Password.HashedPassword,
    var active: Boolean,
    var confirmed: Boolean,
    var roles: Array<Role>,
    val created: Long
) : Entity(id) {

    companion object {
        /**
         * Creates a new [User] object.
         */
        fun create(email: String, password: Password.HashedPassword, active: Boolean, confirmed: Boolean, roles: Array<Role>) =
            User(UUID.randomUUID().toString(), email, password, active, confirmed, roles, System.nanoTime())
    }

    /**
     * Calculates and returns a nonce for this [User] based on its [User.email] and [User.created].
     *
     * @return Nonce for this [User] object.
     */
    fun nonce(): Long {
        val crc = CRC32()
        crc.update(this.email.toByteArray())
        crc.update(this.created.toString().toByteArray())
        return crc.value
    }
}