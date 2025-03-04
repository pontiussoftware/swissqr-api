package ch.pontius.swissqr.api.model.service.bill

import ch.pontius.swissqr.api.utilities.serializers.BigDecimalSerializer
import kotlinx.serialization.Serializable
import net.codecrete.qrbill.generator.Bill
import java.lang.IllegalArgumentException
import java.math.BigDecimal

/**
 * A QR bill which acts as an information vessel during communication and allows for de-/serialization from to JSON.
 * Can be converted to a [net.codecrete.qrbill.generator.Bill]
 *
 * @author Ralph Gasser
 * @version 2.0.0
 */
@Serializable
data class Bill(
    @Serializable(with = BigDecimalSerializer::class)
    val amount: BigDecimal,
    val currency: String,
    val account: String,
    val creditor: Address,
    val debtor: Address,
    val message: String? = null,
    val billInformation: String? = null,
    val reference: String? = null
) {
    /**
     * Constructor to convert [Bill] to [Bill].
     *
     * @param b [Bill]
     */
    constructor(b: Bill): this(
        amount = b.amount,
        currency = b.currency,
        account = b.account,
        message = b.unstructuredMessage,
        billInformation = b.billInformation,
        reference = b.reference,
        debtor = when(b.debtor.type) {
            net.codecrete.qrbill.generator.Address.Type.STRUCTURED -> Address.Structured(b.debtor)
            net.codecrete.qrbill.generator.Address.Type.COMBINED_ELEMENTS -> Address.Combined(b.debtor)
            else -> throw IllegalArgumentException("Provided debtor address format ${b.debtor.type} is not supported.")
        },
        creditor = when(b.creditor.type) {
            net.codecrete.qrbill.generator.Address.Type.STRUCTURED -> Address.Structured(b.creditor)
            net.codecrete.qrbill.generator.Address.Type.COMBINED_ELEMENTS -> Address.Combined(b.creditor)
            else -> throw IllegalArgumentException("Provided creditor address format ${b.creditor.type} is not supported.")
        }
    )


    /**
     * Converts this [Bill] to an object that can be processed by the QR code generator.
     *
     * @return [Bill] representation of this [Bill]
     */
    fun toProcessableBill(): Bill  {
        val bill = Bill()
        bill.account = this.account
        bill.amount = this.amount
        bill.creditor = this.creditor.toProcessableAddress()
        bill.debtor = this.debtor.toProcessableAddress()
        bill.unstructuredMessage = this.message
        bill.billInformation = this.billInformation
        bill.reference = this.reference
        bill.updateReferenceType()
        return bill
    }
}