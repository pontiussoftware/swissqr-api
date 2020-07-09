package ch.pontius.`swissqr-api`.model.bill

import net.codecrete.qrbill.generator.Bill.*

/**
 * Types of references suppored by Swiss QR.
 *
 * @author Ralph Gasser
 * @version 1.0
 */
enum class RefType(val value: String) {
    NO_REF(REFERENCE_TYPE_NO_REF),
    QR_REF(REFERENCE_TYPE_QR_REF),
    CRED_REF(REFERENCE_TYPE_CRED_REF)
}