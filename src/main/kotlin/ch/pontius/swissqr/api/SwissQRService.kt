package ch.pontius.swissqr.api

import ch.pontius.swissqr.api.basics.*
import ch.pontius.swissqr.api.db.DataAccessLayer
import ch.pontius.swissqr.api.handlers.qr.GenerateQRCodeHandler
import ch.pontius.swissqr.api.handlers.qr.GenerateQRCodeSimpleHandler
import ch.pontius.swissqr.api.handlers.qr.ScanQRCodeHandler
import ch.pontius.swissqr.api.model.service.status.Status
import ch.pontius.swissqr.api.model.Config
import ch.pontius.swissqr.api.model.access.Access
import ch.pontius.swissqr.api.model.users.Token
import io.javalin.Javalin
import io.javalin.apibuilder.ApiBuilder
import io.javalin.apibuilder.ApiBuilder.path
import io.javalin.plugin.openapi.OpenApiOptions
import io.javalin.plugin.openapi.OpenApiPlugin
import io.javalin.plugin.openapi.ui.SwaggerOptions
import io.swagger.util.Json
import io.swagger.v3.oas.models.info.Contact
import io.swagger.v3.oas.models.info.Info
import org.slf4j.LoggerFactory
import java.io.File
import java.io.IOException
import java.nio.file.Paths

/**
 * Entry point for Swiss QR API Service.
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
fun main(args: Array<String>) {

    val logger = LoggerFactory.getLogger("ch.pontius.swissqr.api.Main")

    /* Path to config file (defaults to ./config.json) */
    val configPath = args.firstOrNull() ?: "./config.json"
    val config = try {
        Json.mapper().readValue(File(configPath), Config::class.java)
    } catch (e: IOException) {
        logger.error("Config file $configPath not found. Fallback to default config.")
        Config()
    }

    /* Initialize data access layer. */
    val dataAccessLayer = DataAccessLayer(Paths.get(config.data))

    /* Prepare list of HTTP handlers. */
    val handlers = listOf(
        GenerateQRCodeHandler(),
        GenerateQRCodeSimpleHandler(),
        ScanQRCodeHandler()
    )

    /* Initialize Javalin. */
    Javalin.create { c ->
        c.registerPlugin(OpenApiPlugin(getOpenApiOptions()))
        c.defaultContentType = "application/json"
        c.accessManager(AccessManager(dataAccessLayer.tokenStore))
        c.enableCorsForAllOrigins()
        c.requestLogger { ctx, f -> logger.info("Request ${ctx.req.requestURI} completed in ${f}ms.")}
    }.routes {
        path("api") {
            handlers.forEach { handler ->
                path(handler.route) {
                    val permittedRoles = if (handler is AccessManagedRestHandler) {
                        handler.permittedRoles
                    } else {
                        emptySet()
                    }

                    if (handler is GetRestHandler){
                        ApiBuilder.get(handler::get, permittedRoles)
                    }

                    if (handler is PostRestHandler){
                        ApiBuilder.post(handler::post, permittedRoles)
                    }

                    if (handler is PatchRestHandler){
                        ApiBuilder.patch(handler::patch, permittedRoles)
                    }

                    if (handler is DeleteRestHandler){
                        ApiBuilder.delete(handler::delete, permittedRoles)
                    }
                }
            }
        }
    }.before {
        logger.info("${it.req.method} request to ${it.path()} with params (${it.queryParamMap().map { e -> "${e.key}=${e.value}" }.joinToString()}) from ${it.req.remoteAddr}")
    }.after {
        val token = it.attribute<Token>(AccessManager.TOKEN_ATTRIBUTE)
        if (token is Token) {
            dataAccessLayer.accessLogs.append(Access(token.id, it.ip(), it.method(), it.path(), it.status(), System.currentTimeMillis()))
        }
    }.error(401) {
        it.json(Status.ErrorStatus(401, "Unauthorized request!"))
    }.exception(Exception::class.java) { e, ctx ->
        ctx.status(500).json(Status.ErrorStatus(500, "Internal server error: ${e.message}"))
        logger.error("Exception during handling of request to ${ctx.path()}", e)
    }.start(config.port)
}


private fun getOpenApiOptions(): OpenApiOptions {
    val contact = Contact()
    contact.name = "pontius software GmbH"
    contact.email = "rg@pontius.ch"
    contact.url = "https://www.pontius.ch"

    val applicationInfo: Info = Info()
        .version("1.0.0")
        .title("Swiss QR Web Service")
        .description("A web service that can be used to generate and read Swiss QR codes.")
        .contact(contact)

    return OpenApiOptions(applicationInfo).apply {
        path("/swagger-docs") // endpoint for OpenAPI json
        swagger(SwaggerOptions("/swagger-ui"))
        activateAnnotationScanningFor("ch.pontius.swissqr.api.handlers.qr")
    }
}