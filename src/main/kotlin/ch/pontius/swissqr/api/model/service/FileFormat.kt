package ch.pontius.swissqr.api.model.service

import io.swagger.annotations.ApiModel

/**
 * File formats supported by this web service.
 *
 * @author Ralph Gasser
 * @version 1.0
 */
@ApiModel
enum class FileFormat {
    PDF, SVG, PNG
}