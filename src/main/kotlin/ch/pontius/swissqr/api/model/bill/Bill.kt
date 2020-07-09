package ch.pontius.swissqr.api.model.bill

import com.fasterxml.jackson.annotation.JsonProperty
import net.codecrete.qrbill.generator.Bill
import java.lang.IllegalArgumentException
import java.math.BigDecimal

/**
 * A QR bill which acts as an information vessel during communication and allows for de-/serialization from to JSON.
 * Can be converted to a [net.codecrete.qrbill.generator.Bill]
 *
 * @author Ralph Gasser
 * @version 1.0
 */
data class Bill(
    @JsonProperty("amount") val amount: BigDecimal,
    @JsonProperty("currency") val currency: String,
    @JsonProperty("account") val account: String,
    @JsonProperty("creditor") val creditor: Address,
    @JsonProperty("debtor") val debtor: Address,
    @JsonProperty("message") val message: String? = null,
    @JsonProperty("billInformation")  val billInformation: String? = null,
    @JsonProperty("reference")  val reference: String? = null,
    @JsonProperty("referenceType") val referenceType: RefType = RefType.NO_REF
) {

    /**
     * Constructor to convert [net.codecrete.qrbill.generator.Bill] to [Bill].
     *
     * @param b [net.codecrete.qrbill.generator.Bill]
     */
    constructor(b: net.codecrete.qrbill.generator.Bill): this(
        amount = b.amount,
        currency = b.currency,
        account = b.account,
        message = b.unstructuredMessage,
        billInformation = b.billInformation,
        reference =  b.reference,
        referenceType = when(b.referenceType) {
            Bill.REFERENCE_TYPE_NO_REF -> RefType.NO_REF
            Bill.REFERENCE_TYPE_QR_REF -> RefType.QR_REF
            Bill.REFERENCE_TYPE_CRED_REF -> RefType.CRED_REF
            else -> throw IllegalArgumentException("Provided reference type ${b.referenceType} is not supported.")
        },
        debtor = when(b.debtor.type) {
            net.codecrete.qrbill.generator.Address.Type.STRUCTURED -> Address.StructuredAddress(
                b.debtor
            )
            net.codecrete.qrbill.generator.Address.Type.COMBINED_ELEMENTS -> Address.CombinedAddress(
                b.debtor
            )
            else -> throw IllegalArgumentException("Provided debtor address format ${b.debtor.type} is not supported.")
        },
        creditor = when(b.creditor.type) {
            net.codecrete.qrbill.generator.Address.Type.STRUCTURED -> Address.StructuredAddress(
                b.creditor
            )
            net.codecrete.qrbill.generator.Address.Type.COMBINED_ELEMENTS -> Address.CombinedAddress(
                b.creditor
            )
            else -> throw IllegalArgumentException("Provided creditor address format ${b.creditor.type} is not supported.")
        }
    )


    /**
     * Converts this [Bill] to an object that can be processed by the QR code generator.
     *
     * @return [net.codecrete.qrbill.generator.Bill] representation of this [Bill]
     */
    fun toProcessableBill(): net.codecrete.qrbill.generator.Bill  {
        val bill = net.codecrete.qrbill.generator.Bill()
        bill.account = this.account
        bill.amount = this.amount
        bill.creditor = this.creditor.toProcessableAddress()
        bill.debtor = this.creditor.toProcessableAddress()
        bill.unstructuredMessage = this.message
        bill.billInformation = this.billInformation
        bill.reference = this.reference
        bill.referenceType = this.referenceType.value
        return bill
    }
}