package ch.pontius.swissqr.api.handlers.qr

import ch.pontius.swissqr.api.handlers.API_KEY_HEADER
import ch.pontius.swissqr.api.handlers.API_KEY_PARAM
import ch.pontius.swissqr.api.model.service.bill.Address
import ch.pontius.swissqr.api.model.service.bill.Bill
import ch.pontius.swissqr.api.model.service.status.ErrorStatus
import ch.pontius.swissqr.api.model.service.status.ErrorStatusException
import io.javalin.http.BadRequestResponse
import io.javalin.http.Context
import io.javalin.openapi.*
import net.codecrete.qrbill.canvas.PDFCanvas
import net.codecrete.qrbill.canvas.PNGCanvas
import net.codecrete.qrbill.generator.GraphicsFormat
import net.codecrete.qrbill.generator.Language
import net.codecrete.qrbill.generator.OutputSize
import net.codecrete.qrbill.generator.QRBill
import java.math.BigDecimal


@OpenApi(
    summary = "Generates a new unstructured (simple) QR code with the information provided via GET. Important: This type of QR code should not be used after September 2025.",
    path = "/api/qr/simple/{type}",
    methods = [HttpMethod.GET],
    pathParams = [
        OpenApiParam("type", OutputSize::class, "Type of generated invoice. Can either be A4_PORTRAIT_SHEET, QR_BILL_ONLY, QR_CODE_ONLY, QR_BILL_EXTRA_SPACE, QR_BILL_WITH_HORIZONTAL_LINE or PAYMENT_PART_ONLY."),
    ],
    queryParams = [
        OpenApiParam("amount", Double::class, "Amount to be printed on the invoice.", required = true),
        OpenApiParam("account", String::class, "Account number to be printed on the invoice.", required = true),
        OpenApiParam("currency", String::class, "Currency of the amount printed on the invoice.", required = true),
        OpenApiParam("message", String::class, "Message to be printed on the invoice."),
        OpenApiParam("billInformation", String::class, "Bill information to be printed on the invoice."),
        OpenApiParam("reference", String::class, "Currency of the amount printed on the invoice."),
        OpenApiParam("debtor_name", String::class, "Name of the debtor to be printed on the invoice.", required = true),
        OpenApiParam("debtor_country_code", String::class, "Country of the debtor to be printed on the invoice.", required = true),
        OpenApiParam("debtor_address_line_1", String::class, "Address line 1 of the debtor to be printed on the invoice.", required = true),
        OpenApiParam("debtor_address_line_2", String::class, "Address line 2 of the debtor to be printed on the invoice.", required = true),
        OpenApiParam("creditor_name", String::class, "Name of the creditor to be printed on the invoice.", required = true),
        OpenApiParam("creditor_country_code", String::class, "Country of the creditor to be printed on the invoice.", required = true),
        OpenApiParam("creditor_address_line_1", String::class, "Address line 1 of the creditor to be printed on the invoice.", required = true),
        OpenApiParam("creditor_address_line_2", String::class, "Address line 2 of the creditor to be printed on the invoice.", required = true),
        OpenApiParam("format", GraphicsFormat::class, "File format of the resulting QR code (PNG, PDF or SVG). Defaults to PNG."),
        OpenApiParam("language", Language::class, "Language of the generated bill. Options are DE, FR, IT or EN. Defaults to EN."),
        OpenApiParam("resolution", Int::class, "Resolution of the resulting image in dpi. Defaults to 150."),
        OpenApiParam("width", Double::class, "Width of the resulting QR code in mm. Default depending on type."),
        OpenApiParam("height", Double::class, "Height of the resulting QR code in mm. Default depending on type."),
        OpenApiParam(API_KEY_PARAM, String::class, description = "API Token used for authentication. Must be either set in URL or header.", required = false)
    ],
    tags = ["QR Generator"],
    responses = [
        OpenApiResponse("200", [OpenApiContent(ByteArray::class)]),
        OpenApiResponse("400", [OpenApiContent(ErrorStatus::class)]),
        OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)])
    ]
)
fun getGenerateUnstructuredQRCode(ctx: Context) {

    /* Extract invoice data. */
    val bill = Bill(
        account = ctx.queryParam("account") ?: throw ErrorStatusException(400, "Account is required in order to generate QR invoice."),
        amount = BigDecimal(ctx.queryParam("amount") ?: throw ErrorStatusException(400, "Amount is required in order to generate QR invoice.")),
        currency = ctx.queryParam("currency") ?: throw ErrorStatusException(400, "Currency is required in order to generate QR invoice."),
        message = ctx.queryParam("message"),
        billInformation = ctx.queryParam("billInformation"),
        reference = ctx.queryParam("reference"),
        debtor = Address.Combined(
            name = ctx.queryParam("debtor_name") ?: throw ErrorStatusException(400, "Debtor name is required in order to generate QR invoice."),
            countryCode = ctx.queryParam("debtor_country_code") ?: throw ErrorStatusException(400, "Debtor country code is required in order to generate QR invoice."),
            addressLine1 = ctx.queryParam("debtor_address_line_1") ?: throw ErrorStatusException(400, "Debtor address line is required in order to generate QR invoice."),
            addressLine2 = ctx.queryParam("debtor_address_line_2")
        ),
        creditor = Address.Combined(
            name = ctx.queryParam("creditor_name") ?: throw ErrorStatusException(400, "Creditor name is required in order to generate QR invoice."),
            countryCode = ctx.queryParam("creditor_country_code") ?: throw ErrorStatusException(400, "Creditor country code is required in order to generate QR invoice."),
            addressLine1 = ctx.queryParam("creditor_address_line_1") ?: throw ErrorStatusException(400, "Creditor address line is required in order to generate QR invoice."),
            addressLine2 = ctx.queryParam("creditor_address_line_2")
        )
    ).toProcessableBill()


    /* Generate QR code. */
    val data = generateQRCode(bill, ctx)

    /* Write results. */
    ctx.result(data)
}

@OpenApi(
    summary = "Generates a new structured QR code with the information provided via GET.",
    path = "/api/qr/structured/{type}",
    methods = [HttpMethod.GET],
    pathParams = [
        OpenApiParam("type", OutputSize::class, "Type of generated invoice. Can either be A4_PORTRAIT_SHEET, QR_BILL_ONLY, QR_CODE_ONLY, QR_BILL_EXTRA_SPACE, QR_BILL_WITH_HORIZONTAL_LINE or PAYMENT_PART_ONLY."),
    ],
    queryParams = [
        OpenApiParam("amount", Double::class, "Amount to be printed on the invoice.", required = true),
        OpenApiParam("currency", String::class, "Currency of the amount printed on the invoice.", required = true),
        OpenApiParam("account", String::class, "Account number to be printed on the invoice.", required = true),
        OpenApiParam("message", String::class, "Message to be printed on the invoice."),
        OpenApiParam("billInformation", String::class, "Bill information to be printed on the invoice."),
        OpenApiParam("reference", String::class, "Currency of the amount printed on the invoice."),
        OpenApiParam("debtor.name", String::class, "Name of the debtor to be printed on the invoice.", required = true),
        OpenApiParam("debtor.country_code", String::class, "Country of the debtor to be printed on the invoice.", required = true),
        OpenApiParam("debtor.street", String::class, "Street name of the debtor's address to be printed on the invoice.", required = true),
        OpenApiParam("debtor.house_no", String::class, "House number of the debtor's address to be printed on the invoice.", required = true),
        OpenApiParam("debtor.postal_code", String::class, "Postal code of the debtor's address to be printed on the invoice.", required = true),
        OpenApiParam("debtor.town", String::class, "Town of the debtor's address to be printed on the invoice.", required = true),
        OpenApiParam("creditor.name", String::class, "Name of the creditor to be printed on the invoice.", required = true),
        OpenApiParam("creditor.country_code", String::class, "Country of the creditor to be printed on the invoice.", required = true),
        OpenApiParam("creditor.street", String::class, "Street name of the creditor's address.", required = true),
        OpenApiParam("creditor.house_no", String::class, "House number of the creditor's address.", required = true),
        OpenApiParam("creditor.postal_code", String::class, "Postal code of the creditor's address.", required = true),
        OpenApiParam("creditor.town", String::class, "Town of the creditor's address.", required = true),
        OpenApiParam("format", GraphicsFormat::class, "File format of the resulting QR code (PNG, PDF or SVG). Defaults to PNG."),
        OpenApiParam("language", Language::class, "Language of the generated bill. Options are DE, FR, IT or EN. Defaults to EN."),
        OpenApiParam("resolution", Int::class, "Resolution of the resulting image in dpi. Defaults to 150."),
        OpenApiParam("width", Double::class, "Width of the resulting QR code in mm. Default depending on type."),
        OpenApiParam("height", Double::class, "Height of the resulting QR code in mm. Default depending on type."),
        OpenApiParam(API_KEY_PARAM, String::class, description = "API Token used for authentication. Must be either set in URL or header.", required = false)
    ],
    tags = ["QR Generator"],
    responses = [
        OpenApiResponse("200", [OpenApiContent(ByteArray::class)]),
        OpenApiResponse("400", [OpenApiContent(ErrorStatus::class)]),
        OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)])
    ]
)
fun getGenerateStructuredQRCode(ctx: Context) {
    /* Extract invoice from URL parameters. */
    val bill = Bill(
        amount = ctx.queryParam("amount")?.let { BigDecimal(it) } ?: throw ErrorStatusException(400, "Amount is required in order to generate QR invoice."),
        currency = ctx.queryParam("currency") ?: throw ErrorStatusException(400, "Currency is required in order to generate QR invoice."),
        account = ctx.queryParam("account") ?: throw ErrorStatusException(400, "Account is required in order to generate QR invoice."),
        message = ctx.queryParam("message"),
        billInformation = ctx.queryParam("billInformation"),
        reference = ctx.queryParam("reference"),
        debtor = Address.Structured(
            name = ctx.queryParam("debtor.name") ?: throw ErrorStatusException(400, "Debtor name is required in order to generate QR invoice."),
            countryCode = ctx.queryParam("debtor.country_code") ?: throw ErrorStatusException(400, "Debtor country code is required in order to generate QR invoice."),
            street = ctx.queryParam("debtor.street")  ?: throw ErrorStatusException(400, "Debtor street code is required in order to generate QR invoice."),
            houseNo = ctx.queryParam("debtor.house_no") ?: throw ErrorStatusException(400, "Debtor house number is required in order to generate QR invoice."),
            postalCode = ctx.queryParam("debtor.postal_code") ?: throw ErrorStatusException(400, "Debtor postal code is required in order to generate QR invoice."),
            town = ctx.queryParam("debtor.town") ?: throw ErrorStatusException(400, "Debtor postal town is required in order to generate QR invoice.")
        ),
        creditor = Address.Structured(
            name = ctx.queryParam("creditor.name") ?: throw ErrorStatusException(400, "Creditor name is required in order to generate QR invoice."),
            countryCode = ctx.queryParam("creditor.country_code") ?: throw ErrorStatusException(400, "Creditor country code is required in order to generate QR invoice."),
            street = ctx.queryParam("creditor.street")  ?: throw ErrorStatusException(400, "Creditor street code is required in order to generate QR invoice."),
            houseNo = ctx.queryParam("creditor.house_no") ?: throw ErrorStatusException(400, "Creditor house number is required in order to generate QR invoice."),
            postalCode = ctx.queryParam("creditor.postal_code") ?: throw ErrorStatusException(400, "Creditor postal code is required in order to generate QR invoice."),
            town = ctx.queryParam("creditor.town") ?: throw ErrorStatusException(400, "Creditor postal town is required in order to generate QR invoice.")
        )
    ).toProcessableBill()

    /* Generate QR code. */
    val data = generateQRCode(bill, ctx)

    /* Write results. */
    ctx.result(data)
}


@OpenApi(
    summary = "Generates a new structured QR code with the information provided via POST.",
    path = "/api/qr/structured/{type}",
    methods = [HttpMethod.POST],
    headers = [
        OpenApiParam(API_KEY_HEADER, String::class, description = "API Token used for authentication. Syntax: Bearer <Token>", required = true)
    ],
    pathParams = [
        OpenApiParam("type", OutputSize::class, "Type of generated invoice. Can either be A4_PORTRAIT_SHEET, QR_BILL_ONLY, QR_CODE_ONLY, QR_BILL_EXTRA_SPACE, QR_BILL_WITH_HORIZONTAL_LINE or PAYMENT_PART_ONLY."),
    ],
    queryParams = [
        OpenApiParam("format", GraphicsFormat::class, "File format of the resulting QR code (PNG, PDF or SVG). Defaults to PNG."),
        OpenApiParam("language", Language::class, "Language of the generated bill. Options are DE, FR, IT or EN. Defaults to EN."),
        OpenApiParam("width", Double::class, "Width of the resulting QR code in mm. Default depending on type."),
        OpenApiParam("height", Double::class, "Height of the resulting QR code in mm. Default depending on type."),
        OpenApiParam("resolution", Int::class, "Resolution of the resulting QR code in mm. Defaults to 150."),
    ],
    requestBody = OpenApiRequestBody([OpenApiContent(Bill::class)]),
    tags = ["QR Generator"],
    responses = [
        OpenApiResponse("200", [OpenApiContent(ByteArray::class)]),
        OpenApiResponse("400", [OpenApiContent(ErrorStatus::class)]),
        OpenApiResponse("401", [OpenApiContent(ErrorStatus::class)])
    ]
)
fun postGenerateStructuredQRCode(ctx: Context) {
    /* Extract invoice from request body. */
    val bill = try {
        ctx.bodyAsClass(Bill::class.java).toProcessableBill()
    } catch (_: BadRequestResponse){
        throw ErrorStatusException(400, "HTTP body could not be parsed into a valid QR invoice model!")
    }

    /* Generate QR code. */
    val data = generateQRCode(bill, ctx)

    /* Write results. */
    ctx.result(data)
}

/**
 * Generates and returns a QR code for the given [net.codecrete.qrbill.generator.Bill] and [Context].
 *
 * @param bill [net.codecrete.qrbill.generator.Bill] as parsed from a request.
 * @param ctx [Context] of the request (used to extract remaining parameters).
 * @return Byte array containing the generated QR code.
 */
private fun generateQRCode(bill: net.codecrete.qrbill.generator.Bill, ctx: Context): ByteArray {
    /* Extract and validate format parameters. */
    try {
        bill.format.outputSize = OutputSize.valueOf(ctx.pathParamMap().getOrDefault("type", "QR_BILL_ONLY").uppercase())
    } catch (_: IllegalArgumentException) {
        throw ErrorStatusException(400, "Illegal value for parameter 'type'. Possible values are A4_PORTRAIT_SHEET, QR_BILL_ONLY, QR_CODE_ONLY, QR_BILL_EXTRA_SPACE, QR_BILL_WITH_HORIZONTAL_LINE or PAYMENT_PART_ONLY!")
    }

    try {
        bill.format.language = Language.valueOf(ctx.queryParam("language")?.uppercase() ?: "EN")
    } catch (_: IllegalArgumentException) {
        throw ErrorStatusException(400, "Illegal value for parameter 'language'. Possible values are DE, FR, IT or EN!")
    }

    val outputSize = bill.format.outputSize ?: throw ErrorStatusException(500, "Bill format not specified! This is a programmers error.")
    val width = ctx.queryParam("width")?.toDoubleOrNull() ?: when(outputSize) {
        OutputSize.A4_PORTRAIT_SHEET -> 210.0
        OutputSize.QR_BILL_ONLY -> 210.0
        OutputSize.QR_BILL_EXTRA_SPACE -> 210.0
        OutputSize.QR_CODE_ONLY -> 46.0
        OutputSize.QR_CODE_WITH_QUIET_ZONE -> 56.0
        OutputSize.PAYMENT_PART_ONLY -> 148.0
    }
    val height = ctx.queryParam("height")?.toDoubleOrNull() ?: when(outputSize) {
        OutputSize.A4_PORTRAIT_SHEET -> 297.0
        OutputSize.QR_BILL_EXTRA_SPACE -> 110.0
        OutputSize.QR_BILL_ONLY -> 105.0
        OutputSize.QR_CODE_ONLY -> 46.0
        OutputSize.QR_CODE_WITH_QUIET_ZONE -> 56.0
        OutputSize.PAYMENT_PART_ONLY -> 105.0
    }

    val resolution = ctx.queryParam("resolution")?.toIntOrNull() ?: 150

    if (width < 0.0 || height < 0.0) {
        throw ErrorStatusException(400, "Specified format is invalid: Width and height must be greater than zero.")
    }
    if (resolution < 144 || resolution >= 600) {
        throw ErrorStatusException(400, "Specified format is invalid: Resolution must be between 144 and 600dpi.")
    }

    /* Validate QR bill. */
    val validation = QRBill.validate(bill)
    if (!validation.isValid) {
        throw ErrorStatusException(400, "Specified bill is not valid: ${validation.validationMessages.joinToString("\n\n") { it.messageKey }}")
    }

    /* Generate QR code. */
    val format = GraphicsFormat.valueOf(ctx.queryParam("format")?.uppercase() ?: "PNG")
    return when(format) {
        GraphicsFormat.PNG -> {
            bill.format.graphicsFormat = GraphicsFormat.PNG
            val canvas = PNGCanvas(width, height, resolution, "Arial, Helvetica, sans-serif")
            QRBill.draw(bill, canvas)
            ctx.contentType("image/png")
            canvas.toByteArray()
        }
        GraphicsFormat.PDF -> {
            bill.format.graphicsFormat = GraphicsFormat.PDF
            val canvas = PDFCanvas(width, height)
            QRBill.draw(bill, canvas)
            ctx.contentType("application/pdf")
            canvas.toByteArray()
        }
        GraphicsFormat.SVG -> {
            bill.format.graphicsFormat = GraphicsFormat.SVG
            ctx.contentType("image/svg")
            QRBill.generate(bill)
        }
    }
}