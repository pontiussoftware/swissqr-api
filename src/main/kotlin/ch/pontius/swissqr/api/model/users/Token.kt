package ch.pontius.swissqr.api.model.users

import ch.pontius.swissqr.api.model.Entity
import org.mapdb.DataInput2
import org.mapdb.DataOutput2

/**
 * An access [Token] for the Swiss QR code API Service.
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
data class Token(override val id: TokenId, val userId: UserId, val active: Boolean, val created: Long, val roles: Array<Permission>): Entity(id) {

    /**
     * Creates a new [Token] for the given [User] with the given [Permission]s.
     *
     * @param user The [User] to create the [Token] for.
     * @param roles The roles to create the [Token] with.
     */
    constructor(user: User, roles: Array<Permission>) : this(TokenId(), user.id, true, System.currentTimeMillis(), roles) {
        require(user.active && user.confirmed) { "Failed to create token: User ${user.id} is inactive." }
        require(this.roles.isNotEmpty()) { "Failed to create token: You must specify at least one role in order to create a token." }
    }

    companion object Serializer: org.mapdb.Serializer<Token> {
        override fun serialize(out: DataOutput2, value: Token) {
            out.writeUTF(value.id.value)
            out.writeUTF(value.userId.value)
            out.writeBoolean(value.active)
            out.packLong(value.created)
            out.packInt(value.roles.size)
            value.roles.forEach { out.packInt(it.ordinal) }
        }

        override fun deserialize(input: DataInput2, available: Int): Token = Token(
            TokenId(input.readUTF()),
            UserId(input.readUTF()),
            input.readBoolean(),
            input.unpackLong(),
            Array(input.unpackInt()) {
                Permission.values()[input.unpackInt()]
            }
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Token

        if (id != other.id) return false
        if (userId != other.userId) return false
        if (active != other.active) return false
        if (!roles.contentEquals(other.roles)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + userId.hashCode()
        result = 31 * result + active.hashCode()
        result = 31 * result + roles.contentHashCode()
        return result
    }
}