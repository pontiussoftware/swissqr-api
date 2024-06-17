package ch.pontius.swissqr.api.model.config

import ch.pontius.swissqr.api.utilities.serializers.PathSerializer
import kotlinx.serialization.Serializable
import java.nio.file.Path

/**
 * Represents a configuration file for Swiss QR Service
 *
 * @author Ralph Gasser
 * @version 1.1.0
 */
@Serializable
data class Config(
    /** [ServerConfig] reference. */
    val server: ServerConfig = ServerConfig(),

    /** List of API keys that are authorized to use this service. */
    val keys: List<String> = emptyList(),

    /** Path to a custom Log4j2 config file (XML). Defaults to null! */
    @Serializable(with = PathSerializer::class)
    val logConfig: Path? = null
)