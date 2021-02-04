package ch.pontius.swissqr.api.model.config

import ch.pontius.swissqr.api.utilities.serializers.PathSerializer
import kotlinx.serialization.Serializable
import java.nio.file.Path
import java.nio.file.Paths

/**
 * Represents a configuration file for Swiss QR Service
 *
 * @author Ralph Gasser
 * @version 1.0.1
 */
@Serializable
data class Config(
    /** [ServerConfig] reference. */
    val server: ServerConfig = ServerConfig(),

    /** Path to the data folder. */
    @Serializable(with = PathSerializer::class)
    val data: Path = Paths.get("./data"),

    /** Path to a custom Log4j2 config file (XML). Defaults to null! */
    val logConfig: Path? = null
)