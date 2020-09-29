package ch.pontius.swissqr.api.handlers.qr

import ch.pontius.swissqr.api.basics.PostRestHandler
import ch.pontius.swissqr.api.model.service.status.ErrorStatusException
import ch.pontius.swissqr.api.model.service.status.Status
import ch.pontius.swissqr.api.model.service.bill.Bill
import ch.pontius.swissqr.api.model.service.FileFormat
import ch.pontius.swissqr.api.model.service.OutputFormat
import io.javalin.http.BadRequestResponse
import io.javalin.http.Context
import io.javalin.plugin.openapi.annotations.*
import net.codecrete.qrbill.canvas.PDFCanvas
import net.codecrete.qrbill.canvas.PNGCanvas
import net.codecrete.qrbill.generator.OutputSize
import net.codecrete.qrbill.generator.QRBill

/**
 * Javalin handler that acts as a swiss QR code generator that can be used via HTTP POST. It supports different address
 * and invoice formats.
 *
 * @author Ralph Gasser
 * @version 1.0
 */
class GenerateQRCodeHandler : PostRestHandler {
    override val route: String = "qr/generate/:type/:format/:width/:height/:resolution"

    @OpenApi(
        summary = "Generates a new QR code with the given information",
        path = "/api/qr/generate/:type/:format/:width/:height/:resolution",
        method = HttpMethod.POST,
        pathParams = [
            OpenApiParam("type", String::class, "Type of generated invoice. Can either be A4, QR_BILL or QR_ONLY. Defaults to QR_BILL"),
            OpenApiParam("format", String::class, "File format of the resulting QR code (PNG, PDF or SVG)."),
            OpenApiParam("width", Double::class, "Width of the resulting QR code in mm."),
            OpenApiParam("height", Double::class, "Height of the resulting QR code in mm."),
            OpenApiParam("resolution", Int::class, "Resolution of the resulting QR code in mm.")
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
            throw ErrorStatusException(
                400,
                "HTTP body could not be parsed into a valid QR invoice model!"
            )
        }

        /* Extract and validate format parameters. */
        val type = OutputFormat.valueOf(ctx.pathParamMap().getOrDefault("type", "QR_BILL"))
        when (type) {
            OutputFormat.A4 -> bill.format.outputSize = OutputSize.A4_PORTRAIT_SHEET
            OutputFormat.QR_BILL -> bill.format.outputSize = OutputSize.QR_BILL_ONLY
            OutputFormat.QR_ONLY -> bill.format.outputSize = OutputSize.QR_CODE_ONLY
        }

        val width = ctx.pathParamMap()["width"]?.toDoubleOrNull() ?: when(type) {
            OutputFormat.A4 -> 210.0
            OutputFormat.QR_BILL -> 210.0
            OutputFormat.QR_ONLY -> 46.0
        }
        val height = ctx.pathParamMap()["height"]?.toDoubleOrNull() ?: when(type) {
            OutputFormat.A4 -> 297.0
            OutputFormat.QR_BILL -> 105.0
            OutputFormat.QR_ONLY -> 46.0
        }
        val resolution = ctx.pathParamMap()["resolution"]?.toIntOrNull() ?: 150

        if (width < 0.0 || height < 0.0) {
            throw ErrorStatusException(
                400,
                "Specified format is invalid: Width and height must be greater than zero."
            )
        }
        if (resolution < 144 || resolution >= 600) {
            throw ErrorStatusException(
                400,
                "Specified format is invalid: Resolution but be between 144 and 600dpi must be greater than zero."
            )
        }

        /* Validate QR bill. */
        val validation = QRBill.validate(bill)
        if (!validation.isValid) {
            throw ErrorStatusException(
                400,
                "Specified bill is not valid: ${validation.validationMessages.joinToString("\n\n") { it.messageKey }}"
            )
        }

        /* Generate QR code. */
        val data = when(FileFormat.valueOf(ctx.pathParamMap().getOrDefault("format", "PNG"))) {
            FileFormat.PNG -> {
                val canvas = PNGCanvas(width, height, resolution, "Arial, Helvetica, sans-serif")
                QRBill.draw(bill, canvas)
                ctx.contentType("image/png")
                canvas.toByteArray()
            }
            FileFormat.PDF -> {
                val canvas = PDFCanvas(width, height)
                QRBill.draw(bill, canvas)
                ctx.contentType("application/pdf")
                canvas.toByteArray()
            }
            FileFormat.SVG -> {
                ctx.contentType("image/svg")
                QRBill.generate(bill)
            }
        }

        /* Write results. */
        ctx.result(data)
    }
}