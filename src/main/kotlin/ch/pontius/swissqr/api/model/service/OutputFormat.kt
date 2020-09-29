package ch.pontius.swissqr.api.model.service

import io.swagger.annotations.ApiModel

/**
 * Output formats supported by this web sevice.
 *
 * @author Ralph Gasser
 * @version 1.0
 */
@ApiModel
enum class OutputFormat {
    A4, QR_BILL, QR_ONLY
}