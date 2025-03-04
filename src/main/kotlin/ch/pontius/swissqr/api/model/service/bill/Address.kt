package ch.pontius.swissqr.api.model.service.bill


import io.javalin.openapi.OneOf
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * An address as used on a QR coded bill and acts as an information vessel during communication and allows for
 * de-/serialization from to JSON. Can either be [Structured] or [Combined].
 *
 * @see Bill
 * @author Ralph Gasser
 * @version 1.0.0
 */
@Serializable
@OneOf(Address.Structured::class, Address.Combined::class)
sealed interface Address {

    val name: String


    val countryCode: String


    fun toProcessableAddress(): net.codecrete.qrbill.generator.Address

    /**
     * Structured [Address]
     */
    @Serializable
    @SerialName("structured")
    data class Structured(
        override val name: String,
        override val countryCode:
        String, val street: String,
        val houseNo: String,
        val postalCode: String,
        val town: String
    ) : Address {

        constructor(a: net.codecrete.qrbill.generator.Address): this(
            name = a.name,
            countryCode = a.countryCode,
            street = a.street,
            houseNo = a.houseNo,
            postalCode = a.postalCode,
            town = a.town
        )

        override fun toProcessableAddress(): net.codecrete.qrbill.generator.Address {
            val address = net.codecrete.qrbill.generator.Address()
            address.countryCode = this.countryCode
            address.name = this.name
            address.street = this.street
            address.houseNo = this.houseNo
            address.postalCode = this.postalCode
            address.town = this.town
            return address
        }
    }

    /**
     * Unstructured [Address]
     */
    @Serializable
    @SerialName("unstructured")
    data class Combined(
        override val name: String,
        override val countryCode: String,
        val addressLine1: String,
        val addressLine2: String?
    ) : Address {

        constructor(a: net.codecrete.qrbill.generator.Address): this(
            name = a.name,
            countryCode = a.countryCode,
            addressLine1 = a.addressLine1,
            addressLine2 = a.addressLine2
        )

        override fun toProcessableAddress(): net.codecrete.qrbill.generator.Address {
            val address = net.codecrete.qrbill.generator.Address()
            address.countryCode = this.countryCode
            address.name = this.name
            address.addressLine1 = this.addressLine1
            address.addressLine2 = this.addressLine2
            return address
        }
    }
}