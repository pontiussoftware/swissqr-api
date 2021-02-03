package ch.pontius.swissqr.api.model.access

import ch.pontius.swissqr.api.model.users.TokenId
import org.mapdb.DataInput2
import org.mapdb.DataOutput2

/**
 * Represents an [Access] to the Swiss QR code API.
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
data class Access(val tokenId: TokenId, val ip: String, val path: String, val method: String, val status: Int, val timestamp: Long = System.currentTimeMillis()) {

    companion object Serializer: org.mapdb.Serializer<Access> {
        override fun serialize(out: DataOutput2, value: Access) {
            out.writeUTF(value.tokenId.value)
            out.writeUTF(value.ip)
            out.writeUTF(value.path)
            out.writeUTF(value.method)
            out.packInt(value.status)
            out.packLong(value.timestamp)
        }

        override fun deserialize(input: DataInput2, available: Int): Access = Access(
            TokenId(input.readUTF()),
            input.readUTF(),
            input.readUTF(),
            input.readUTF(),
            input.unpackInt(),
            input.unpackLong()
        )
    }
}