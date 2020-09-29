package ch.pontius.swissqr.api.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Represents a configuration file for Swiss QR API
 *
 * @author Ralph Gasser
 * @version 1.0
 */
data class Config(
    @JsonProperty("port") val port: Int = 8080,
    @JsonProperty("data") val data: String = "./data"
)