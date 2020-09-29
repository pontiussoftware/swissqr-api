package ch.pontius.swissqr.api.handlers.qr

import boofcv.factory.fiducial.FactoryFiducial
import boofcv.io.image.ConvertBufferedImage
import boofcv.struct.image.GrayU8
import ch.pontius.swissqr.api.basics.PostRestHandler
import ch.pontius.swissqr.api.model.service.status.ErrorStatusException
import ch.pontius.swissqr.api.model.service.status.Status
import ch.pontius.swissqr.api.model.service.bill.Bill
import io.javalin.http.Context
import io.javalin.plugin.openapi.annotations.*
import net.codecrete.qrbill.generator.QRBill
import net.codecrete.qrbill.generator.QRBillValidationError
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.rendering.ImageType
import org.apache.pdfbox.rendering.PDFRenderer
import java.awt.image.BufferedImage
import java.io.IOException
import javax.imageio.ImageIO

/**
 * Javalin handler that scans QR codes from PDFs or images, reads their data and returns a list [Bill] data structures.
 *
 * @author Ralph Gasser
 * @version 1.0
 */
class ScanQRCodeHandler : PostRestHandler {

    override val route: String
        get() = "qr/scan"

    @OpenApi(
        summary = "Scans a swiss QR code with the given information and returns the data it contains.",
        path = "/api/qr/scan",
        method = HttpMethod.POST,
        requestBody = OpenApiRequestBody(content = [
            OpenApiContent(from = ByteArray::class, type = "image/png"),
            OpenApiContent(from = ByteArray::class, type = "image/jpg"),
            OpenApiContent(from = ByteArray::class, type = "application/pdf")
        ]),
        tags = ["QR Scanner"],
        responses = [
            OpenApiResponse("200", [OpenApiContent(Array<Bill>::class)]),
            OpenApiResponse("400", [OpenApiContent(Status.ErrorStatus::class)]),
            OpenApiResponse("401", [OpenApiContent(Status.ErrorStatus::class)])
        ]
    )
    override fun doPost(ctx: Context) {

        /* Extract images that should be scanned. */
        val images = extractImage(ctx)
        val detector = FactoryFiducial.qrcode(null, GrayU8::class.java)
        val bills = images.flatMap { img ->
            val gray8 = ConvertBufferedImage.convertFrom(img, null as GrayU8?)
            detector.process(gray8)
            detector.detections
        }.mapNotNull {
            try {
                Bill(QRBill.decodeQrCodeText(it.message))
            } catch (e: QRBillValidationError) {
                null
            }
        }.toTypedArray()

        /* Report error if no result could be found. */
        if (bills.isEmpty()) {
            throw ErrorStatusException(
                404,
                "Provided data did not contain any Swiss QR code."
            )
        }

        /* Write results to stream. */
        ctx.res.contentType = "application/json"
        ctx.json(bills)
    }

    /**
     * Extracts a list of [BufferedImage]s from the given [Context]'s HTTP request.
     *
     * @param ctx The [Context] to extract the images from.
     */
    private fun extractImage(ctx: Context): List<BufferedImage> = when(ctx.req.contentType) {
        "image/jpeg", "image/jpg", "image/png" -> listOf(ImageIO.read(ctx.req.inputStream))
        "application/pdf" -> {
            try {
                val doc = PDDocument.load(ctx.req.inputStream)
                val renderer = PDFRenderer(doc)
                val numberOfPages: Int = doc.getNumberOfPages()
                val result = (0 until numberOfPages).map {
                    renderer.renderImageWithDPI(it, 300.0f, ImageType.RGB)
                }
                doc.close()
                result
            } catch (e: IOException) {
                throw ErrorStatusException(
                    400,
                    "Failed to read provided PDF document due to IOException: ${e.message}"
                )
            }
        }
        else -> throw ErrorStatusException(
            400,
            "Provided content type '${ctx.req.contentType} is not supported!"
        )
    }
}