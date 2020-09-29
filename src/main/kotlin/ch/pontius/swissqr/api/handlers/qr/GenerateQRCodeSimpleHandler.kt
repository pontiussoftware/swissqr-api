package ch.pontius.swissqr.api.handlers.qr

import ch.pontius.swissqr.api.basics.GetRestHandler
import ch.pontius.swissqr.api.model.service.status.ErrorStatusException
import ch.pontius.swissqr.api.model.service.status.Status
import ch.pontius.swissqr.api.model.service.bill.Address
import ch.pontius.swissqr.api.model.service.bill.Bill
import ch.pontius.swissqr.api.model.service.bill.RefType
import ch.pontius.swissqr.api.model.service.OutputFormat

import io.javalin.http.Context
import io.javalin.plugin.openapi.annotations.*

import net.codecrete.qrbill.canvas.PNGCanvas
import net.codecrete.qrbill.generator.OutputSize
import net.codecrete.qrbill.generator.QRBill

import java.math.BigDecimal

/**
 * Javalin handler that acts as a simple swiss QR code generator that can be used via HTTP GET. Its functionality is
 * slightly reduced (e.g. it only generates PNG files and only supports unstructured address format).
 *
 * @author Ralph Gasser
 * @version 1.0
 */
class GenerateQRCodeSimpleHandler : GetRestHandler {
    override val route: String = "qr/simple/:type/:resolution"

    @OpenApi(
        summary = "Generates a new QR code with the given information",
        path = "/api/qr/simple/:type/:resolution",
        method = HttpMethod.GET,
        pathParams = [
            OpenApiParam("type", String::class, "Type of generated invoice. Can either be A4, QR_BILL or QR_ONLY."),
            OpenApiParam("resolution", Int::class, "Resolution of the resulting QR code in dpi. Default ist 150.")
        ],
        queryParams = [
            OpenApiParam("amount", Double::class, "Amount to be printed on the invoice.", required = true),
            OpenApiParam("account", String::class, "Account number to be printed on the invoice.", required = true),
            OpenApiParam("currency", String::class, "Currency of the amount printed on the invoice.", required = true),
            OpenApiParam("message", String::class, "Message to be printed on the invoice."),
            OpenApiParam("billInformation", String::class, "Bill information to be printed on the invoice."),
            OpenApiParam("reference", String::class, "Currency of the amount printed on the invoice"),
            OpenApiParam("debtor_name", String::class, "Name of the debtor to be printed on the invoice", required = true),
            OpenApiParam("debtor_country_code", String::class, "Country of the debtor to be printed on the invoice", required = true),
            OpenApiParam("debtor_address_line_1", String::class, "Address line 1 of the debtor to be printed on the invoice"),
            OpenApiParam("debtor_address_line_2", String::class, "Address line 2 of the debtor to be printed on the invoice"),
            OpenApiParam("creditor_name", String::class, "Name of the creditor to be printed on the invoice", required = true),
            OpenApiParam("creditor_country_code", String::class, "Country of the creditor to be printed on the invoice", required = true),
            OpenApiParam("creditor_address_line_1", String::class, "Address line 1 of the creditor to be printed on the invoice"),
            OpenApiParam("creditor_address_line_2", String::class, "Address line 2 of the creditor to be printed on the invoice")
        ],
        tags = ["QR Generator"],
        responses = [
            OpenApiResponse("200", [OpenApiContent(ByteArray::class)]),
            OpenApiResponse("400", [OpenApiContent(Status.ErrorStatus::class)]),
            OpenApiResponse("401", [OpenApiContent(Status.ErrorStatus::class)])
        ]
    )
    override fun doGet(ctx: Context) {

        /* Extract invoice data. */
        val refTypeString = ctx.queryParam("reference")
        val refType = when {
            refTypeString == null -> RefType.NO_REF
            refTypeString.startsWith("RF") -> RefType.CRED_REF
            else -> RefType.QR_REF
        }

        val bill = Bill(
            account = ctx.queryParam("account")
                ?: throw ErrorStatusException(
                    400,
                    "Account is required in order to generate QR invoice."
                ),
            amount = BigDecimal(
                ctx.queryParam("amount") ?: throw ErrorStatusException(
                    400,
                    "Amount is required in order to generate QR invoice."
                )
            ),
            currency = ctx.queryParam("currency")
                ?: throw ErrorStatusException(
                    400,
                    "Currency is required in order to generate QR invoice."
                ),
            message = ctx.queryParam("message"),
            billInformation = ctx.queryParam("billInformation"),
            reference = ctx.queryParam("reference"),
            debtor = Address.CombinedAddress(
                name = ctx.queryParam("debtor_name")
                    ?: throw ErrorStatusException(
                        400,
                        "Debtor name is required in order to generate QR invoice."
                    ),
                countryCode = ctx.queryParam("debtor_country_code")
                    ?: throw ErrorStatusException(
                        400,
                        "Debtor country code is required in order to generate QR invoice."
                    ),
                addressLine1 = ctx.queryParam("debtor_address_line_1")
                    ?: throw ErrorStatusException(
                        400,
                        "Debtor address line is required in order to generate QR invoice."
                    ),
                addressLine2 = ctx.queryParam("debtor_address_line_2")
            ),
            creditor = Address.CombinedAddress(
                name = ctx.queryParam("creditor_name")
                    ?: throw ErrorStatusException(
                        400,
                        "Creditor name is required in order to generate QR invoice."
                    ),
                countryCode = ctx.queryParam("creditor_country_code")
                    ?: throw ErrorStatusException(
                        400,
                        "Creditor country code is required in order to generate QR invoice."
                    ),
                addressLine1 = ctx.queryParam("creditor_address_line_1")
                    ?: throw ErrorStatusException(
                        400,
                        "Creditor address line is required in order to generate QR invoice."
                    ),
                addressLine2 = ctx.queryParam("creditor_address_line_2")
            ),
            referenceType = refType
        ).toProcessableBill()

        /* Extract and validate format parameters. */
        val type = OutputFormat.valueOf(ctx.pathParamMap().getOrDefault("type", "QR_BILL"))
        when (type) {
            OutputFormat.A4 -> bill.format.outputSize = OutputSize.A4_PORTRAIT_SHEET
            OutputFormat.QR_BILL -> bill.format.outputSize = OutputSize.QR_BILL_ONLY
            OutputFormat.QR_ONLY -> bill.format.outputSize = OutputSize.QR_CODE_ONLY
        }

        /* Determine width, height and resolution. */
        val width = when(type) {
            OutputFormat.A4 -> 210.0
            OutputFormat.QR_BILL -> 210.0
            OutputFormat.QR_ONLY -> 46.0
        }
        val height = when(type) {
            OutputFormat.A4 -> 297.0
            OutputFormat.QR_BILL -> 105.0
            OutputFormat.QR_ONLY -> 46.0
        }
        val resolution = ctx.pathParamMap()["resolution"]?.toIntOrNull() ?: 150

        /* Validate QR bill. */
        val validation = QRBill.validate(bill)
        if (!validation.isValid) {
            throw ErrorStatusException(
                400,
                "Specified bill is not valid: ${validation.validationMessages.joinToString("\n\n") { it.messageKey }}"
            )
        }

       /* Generate QR code. */
        val canvas = PNGCanvas(width, height, resolution, "Arial, Helvetica, sans-serif")
        QRBill.draw(bill, canvas)
        ctx.contentType("image/png")
        ctx.result(canvas.toByteArray())
    }
}