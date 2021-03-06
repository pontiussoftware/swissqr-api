package ch.pontius.swissqr.api.model.users

import ch.pontius.swissqr.api.model.Entity
import org.mapdb.DataInput2
import org.mapdb.DataOutput2
import java.util.zip.CRC32

/**
 * A [User] of the Swiss QR code system.
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
data class User(
    override val id: UserId,
    val email: String,
    val password: Password.HashedPassword,
    val description: String?,
    val active: Boolean,
    val confirmed: Boolean,
    val created: Long
) : Entity(id) {

    /**
     * Creates a new [User] from given user input.
     *
     * @param email The e-mail of the user. Acts as username.
     * @param password The [Password.PlainPassword] of the [User].
     * @param description A textual description of the [User].
     * @param active Whether [User] should be active.
     * @param confirmed Whether [User] should be confirmed.
     */
    constructor(email: String, password: Password.PlainPassword, description: String?, active: Boolean, confirmed: Boolean):
        this(UserId(), email, password.hash(), description, active, confirmed, System.currentTimeMillis())


    /**
     * Serializer for [User] objects.
     */
    companion object Serializer : org.mapdb.Serializer<User> {
        override fun serialize(out: DataOutput2, value: User) {
            out.writeUTF(value.id.value)
            out.writeUTF(value.email)
            out.writeUTF(value.password.value)
            out.writeUTF(value.description ?: "")
            out.writeBoolean(value.active)
            out.writeBoolean(value.confirmed)
            out.packLong(value.created)
        }

        override fun deserialize(input: DataInput2, available: Int): User = User(
            UserId(input.readUTF()),
            input.readUTF(),
            Password.HashedPassword(input.readUTF()),
            input.readUTF().ifEmpty { null },
            input.readBoolean(),
            input.readBoolean(),
            input.unpackLong()
        )
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