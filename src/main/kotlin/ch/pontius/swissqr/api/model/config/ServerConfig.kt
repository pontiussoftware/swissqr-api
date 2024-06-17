package ch.pontius.swissqr.api.model.config

import ch.pontius.swissqr.api.utilities.serializers.PathSerializer
import kotlinx.serialization.Serializable
import java.nio.file.Path

/**
 * Configurations regarding the HTTP server.
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
@Serializable
data class ServerConfig(
    /** Port used for serving HTTP requests. */
    val port: Int = 8080,

    /** Port used for serving HTTPS requests. */
    val sslPort: Int = 8443,

    /** Path to the key store file (required for SSL). */
    @Serializable(with = PathSerializer::class)
    val sslKeyStore: Path? = null,

    /** Keystore password. */
    val sslKeysStorePassword: String?  = null,

    /** Flag indicating whether SSL should be enforced. */
    val sslEnforce: Boolean = false
)