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
data class Token(override val id: TokenId, val userId: UserId, val active: Boolean, val roles: Array<Role>): Entity(id) {
    companion object Serializer: org.mapdb.Serializer<Token> {
        override fun serialize(out: DataOutput2, value: Token) {
            out.writeUTF(value.id.value)
            out.writeUTF(value.userId.value)
            out.writeBoolean(value.active)
            out.packInt(value.roles.size)
            value.roles.forEach { out.packInt(it.ordinal) }
        }

        override fun deserialize(input: DataInput2, available: Int): Token = Token(
            TokenId(input.readUTF()),
            UserId(input.readUTF()),
            input.readBoolean(),
            Array(input.unpackInt()) {
                Role.values()[input.unpackInt()]
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