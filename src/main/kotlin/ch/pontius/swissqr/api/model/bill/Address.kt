package ch.pontius.swissqr.api.model.bill

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

/**
 * A address as used on a QR coded bill and acts as an information vessel during communication and allows for
 * de-/serialization from to JSON.. Can either be [StructuredAddress] or [CombinedAddress].
 *
 * @see Bill
 * @author Ralph Gasser
 * @version 1.0
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(
    JsonSubTypes.Type(value = Address.StructuredAddress::class, name = "structured"),
    JsonSubTypes.Type(value = Address.CombinedAddress::class, name = "combined")
)
sealed class Address(open val name: String, open val countryCode: String) {


    abstract fun toProcessableAddress(): net.codecrete.qrbill.generator.Address

    /**
     * Structured [Address]
     */
    data class StructuredAddress(
        @JsonProperty("name") override val name: String,
        @JsonProperty("countryCode") override val countryCode: String,
        @JsonProperty("street") val street: String,
        @JsonProperty("houseNo") val houseNo: String,
        @JsonProperty("postalCode") val postalCode: String,
        @JsonProperty("town") val town: String
    ) : Address(name, countryCode) {

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
    data class CombinedAddress(
        @JsonProperty("name") override val name: String,
        @JsonProperty("countryCode") override val countryCode: String,
        @JsonProperty("addressLine1") val addressLine1: String,
        @JsonProperty("addressLine2") val addressLine2: String?
    ) : Address(name, countryCode) {

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