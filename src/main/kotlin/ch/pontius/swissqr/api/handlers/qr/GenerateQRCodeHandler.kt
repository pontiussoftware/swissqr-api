package ch.pontius.swissqr.api.handlers.qr

import ch.pontius.swissqr.api.basics.AccessManager
import ch.pontius.swissqr.api.basics.PostRestHandler
import ch.pontius.swissqr.api.model.service.status.ErrorStatusException
import ch.pontius.swissqr.api.model.service.status.Status
import ch.pontius.swissqr.api.model.service.bill.Bill
import ch.pontius.swissqr.api.model.users.Permission
import io.javalin.core.util.Header
import io.javalin.http.BadRequestResponse
import io.javalin.http.Context
import io.javalin.plugin.openapi.annotations.*
import net.codecrete.qrbill.canvas.PDFCanvas
import net.codecrete.qrbill.canvas.PNGCanvas
import net.codecrete.qrbill.generator.GraphicsFormat
import net.codecrete.qrbill.generator.Language
import net.codecrete.qrbill.generator.OutputSize
import net.codecrete.qrbill.generator.QRBill

/**
 * A [PostRestHandler] for Javalin that acts as a swiss QR code generator that can be used via HTTP POST.
 * It supports different address and invoice formats.
 *
 * @author Ralph Gasser
 * @version 1.0.1
 */
class GenerateQRCodeHandler : PostRestHandler {
    /** Path of [GenerateQRCodeSimpleHandler]. */
    override val route: String = "qr/generate/:type"

    /** Set of [Permission]s a user requires in order to be allowed to use [GenerateQRCodeHandler]. */
    override val requiredPermissions: Set<Permission> = setOf(Permission.QR_CREATE)

    @OpenApi(
        summary = "Generates a new QR code with the information provided via POST.",
        path = "/public/qr/generate/:type",
        method = HttpMethod.POST,
        headers = [
            OpenApiParam(AccessManager.API_KEY_HEADER, String::class, description = "API Token used for authentication. Syntax: Bearer <Token>", required = true)
        ],
        pathParams = [
            OpenApiParam("type", String::class, "Type of generated invoice. Can either be A4_PORTRAIT_SHEET, QR_BILL_ONLY, QR_BILL_WITH_HORIZONTAL_LINE or QR_CODE_ONLY."),
        ],
        queryParams = [
            OpenApiParam("format", String::class, "File format of the resulting QR code (PNG, PDF or SVG). Defaults to PNG."),
            OpenApiParam("width", Double::class, "Width of the resulting QR code in mm. Default depending on type."),
            OpenApiParam("height", Double::class, "Height of the resulting QR code in mm. Default depending on type."),
            OpenApiParam("resolution", Int::class, "Resolution of the resulting QR code in mm. Defaults to 150."),
            OpenApiParam("language", String::class, "Language of the generated bill. Options are DE, FR, IT or EN. Defaults to EN.")
        ],
        requestBody = OpenApiRequestBody([OpenApiContent(Bill::class)]),
        tags = ["QR Generator"],
        responses = [
            OpenApiResponse("200", [OpenApiContent(ByteArray::class)]),
            OpenApiResponse("400", [OpenApiContent(Status.ErrorStatus::class)]),
            OpenApiResponse("401", [OpenApiContent(Status.ErrorStatus::class)])
        ]
    )
    override fun doPost(ctx: Context) {

        /* Extract invoice data. */
        val bill = try {
            ctx.bodyAsClass(Bill::class.java).toProcessableBill()
        } catch (e: BadRequestResponse){
            throw ErrorStatusException(400, "HTTP body could not be parsed into a valid QR invoice model!")
        }

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

        val width = ctx.queryParam("width")?.toDoubleOrNull() ?: when(bill.format.outputSize) {
            OutputSize.A4_PORTRAIT_SHEET -> 210.0
            OutputSize.QR_BILL_ONLY -> 210.0
            OutputSize.QR_BILL_WITH_HORIZONTAL_LINE -> 210.0
            OutputSize.QR_CODE_ONLY -> 46.0
        }
        val height = ctx.queryParam("height")?.toDoubleOrNull() ?: when(bill.format.outputSize) {
            OutputSize.A4_PORTRAIT_SHEET -> 297.0
            OutputSize.QR_BILL_WITH_HORIZONTAL_LINE -> 110.0
            OutputSize.QR_BILL_ONLY -> 105.0
            OutputSize.QR_CODE_ONLY -> 46.0
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
        val data = when(GraphicsFormat.valueOf(ctx.queryParam("format", "PNG")!!.toUpperCase())) {
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

        /* Write results. */
        ctx.result(data)
    }
}