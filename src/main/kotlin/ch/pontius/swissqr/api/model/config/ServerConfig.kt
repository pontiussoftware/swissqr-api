package ch.pontius.swissqr.api.model.config

import ch.pontius.swissqr.api.utilities.serializers.PathSerializer
import kotlinx.serialization.Serializable
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.server.ServerConnector
import org.eclipse.jetty.util.ssl.SslContextFactory
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
) {
    /**
     * Generates and returns a Jetty [Server]
     *
     * @return Configured [Server] object
     */
    fun server(): Server {
        val server = Server()
        val connectors = mutableListOf<ServerConnector>()

        /* Prepare default (HTTP) connector. */
        val deaultConntect = ServerConnector(server)
        deaultConntect.port = this.port
        connectors.add(deaultConntect)

        /* Prepare SSL (HTTPS) connector. */
        if (this.sslKeyStore != null) {
            val sslConnector = ServerConnector(server, sslContextFactory())
            sslConnector.port =  this.sslPort
            connectors.add(sslConnector)
        }

        server.connectors = connectors.toTypedArray()
        return server
    }

    /**
     * Generates and returns a Jetty [SslContextFactory]
     *
     * @return Configured [SslContextFactory] object
     */
    fun sslContextFactory(): SslContextFactory {
        check(this.sslKeyStore != null) { "Cannot construct SslContextFactory of nonexistent keystore." }
        val sslContextFactory = SslContextFactory.Server()
        sslContextFactory.keyStorePath = this.sslKeyStore.toString()
        sslContextFactory.setKeyStorePassword(this.sslKeysStorePassword)
        return sslContextFactory
    }
}