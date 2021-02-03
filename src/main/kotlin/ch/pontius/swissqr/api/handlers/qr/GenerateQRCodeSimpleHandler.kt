package ch.pontius.swissqr.api.handlers.qr

import ch.pontius.swissqr.api.basics.AccessManager
import ch.pontius.swissqr.api.basics.GetRestHandler
import ch.pontius.swissqr.api.model.service.status.ErrorStatusException
import ch.pontius.swissqr.api.model.service.status.Status
import ch.pontius.swissqr.api.model.service.bill.Address
import ch.pontius.swissqr.api.model.service.bill.Bill
import ch.pontius.swissqr.api.model.users.Permission
import io.javalin.core.util.Header

import io.javalin.http.Context
import io.javalin.plugin.openapi.annotations.*

import net.codecrete.qrbill.canvas.PNGCanvas
import net.codecrete.qrbill.generator.GraphicsFormat
import net.codecrete.qrbill.generator.Language
import net.codecrete.qrbill.generator.OutputSize
import net.codecrete.qrbill.generator.QRBill

import java.math.BigDecimal

/**
 * A [GetRestHandler] for Javalin that acts as a simple swiss QR code generator that can be invoked via HTTP GET.
 *
 * Its functionality is slightly reduced as compared to [GenerateQRCodeHandler]. For example, it only generates PNG
 * files and only supports an unstructured address format.
 *
 * @author Ralph Gasser
 * @version 1.0.1
 */
class GenerateQRCodeSimpleHandler : GetRestHandler {
    /** Path of [GenerateQRCodeSimpleHandler]. */
    override val route: String = "qr/simple/:type"

    /** Set of [Permission]s a user requires in order to be allowed to use [GenerateQRCodeHandler]. */
    override val requiredPermissions: Set<Permission> = setOf(Permission.QR_CREATE)

    @OpenApi(
        summary = "Generates a new QR code with the information provided via GET.",
        path = "/public/qr/simple/:type",
        method = HttpMethod.GET,
        headers = [
            OpenApiParam(AccessManager.API_KEY_HEADER, String::class, description = "API Token used for authentication. Must be either set in URL or header. Syntax: Bearer <Token>", required = false)
        ],
        pathParams = [
            OpenApiParam("type", String::class, "Type of generated invoice. Can either be A4_PORTRAIT_SHEET, QR_BILL_ONLY, QR_BILL_WITH_HORIZONTAL_LINE or QR_CODE_ONLY."),
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
            OpenApiParam("debtor_address_line_1", String::class, "Address line 1 of the debtor to be printed on the invoice", required = true),
            OpenApiParam("debtor_address_line_2", String::class, "Address line 2 of the debtor to be printed on the invoice", required = true),
            OpenApiParam("creditor_name", String::class, "Name of the creditor to be printed on the invoice", required = true),
            OpenApiParam("creditor_country_code", String::class, "Country of the creditor to be printed on the invoice", required = true),
            OpenApiParam("creditor_address_line_1", String::class, "Address line 1 of the creditor to be printed on the invoice", required = true),
            OpenApiParam("creditor_address_line_2", String::class, "Address line 2 of the creditor to be printed on the invoice", required = true),
            OpenApiParam("language", String::class, "Language of the generated bill. Options are DE, FR, IT or EN. Defaults to EN."),
            OpenApiParam("resolution", Int::class, "Resolution of the resulting image in dpi. Defaults to 150."),
            OpenApiParam("token", String::class, description = "API Token used for authentication. Must be either set in URL or header.", required = false)
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
            )
        ).toProcessableBill()


        /* Extract and validate format parameters. */
        try {
            bill.format.outputSize = OutputSize.valueOf(ctx.pathParamMap().getOrDefault("type", "QR_BILL_ONLY").toUpperCase())
        } catch (e: IllegalArgumentException) {
            throw ErrorStatusException(400, "Illegal value for parameter 'type'. Possible values are A4_PORTRAIT_SHEET, QR_BILL_ONLY, QR_BILL_WITH_HORIZONTAL_LINE or QR_CODE_ONLY!")
        }

        try {
            bill.format.language = Language.valueOf(ctx.queryParam("language", "EN")!!.toUpperCase())
        } catch (e: IllegalArgumentException) {
            throw ErrorStatusException(400, "Illegal value for parameter 'language'. Possible values are DE, FR, IT or EN!")
        }

        /* Determine width, height and resolution. */
        val width = when(bill.format.outputSize ?: throw ErrorStatusException(500, "Bill format not specified! This is a programmers error.")) {
            OutputSize.A4_PORTRAIT_SHEET -> 210.0
            OutputSize.QR_BILL_ONLY -> 210.0
            OutputSize.QR_BILL_WITH_HORIZONTAL_LINE -> 210.0
            OutputSize.QR_CODE_ONLY -> 46.0
        }
        val height = when(bill.format.outputSize ?: throw ErrorStatusException(500, "Bill format not specified! This is a programmers error.")) {
            OutputSize.A4_PORTRAIT_SHEET -> 297.0
            OutputSize.QR_BILL_WITH_HORIZONTAL_LINE -> 110.0
            OutputSize.QR_BILL_ONLY -> 105.0
            OutputSize.QR_CODE_ONLY -> 46.0
        }
        val resolution = ctx.queryParam("resolution")?.toIntOrNull() ?: 150
        if (resolution < 144 || resolution >= 600) {
            throw ErrorStatusException(400, "Specified format is invalid: Resolution must be between 144 and 600dpi.")
        }

        /* Validate QR bill. */
        val validation = QRBill.validate(bill)
        if (!validation.isValid) {
            throw ErrorStatusException(400, "Specified bill is not valid: ${validation.validationMessages.joinToString("\n\n") { it.messageKey }}")
        }

       /* Generate QR code. */
        val canvas = PNGCanvas(width, height, resolution, "Arial, Helvetica, sans-serif")
        bill.format.graphicsFormat = GraphicsFormat.PNG
        QRBill.draw(bill, canvas)
        ctx.contentType("image/png")
        ctx.result(canvas.toByteArray())
    }
}