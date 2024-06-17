package ch.pontius.swissqr.api

import ch.pontius.swissqr.api.handlers.API_KEY_HEADER
import ch.pontius.swissqr.api.handlers.API_KEY_PARAM
import ch.pontius.swissqr.api.handlers.qr.generateQRCode
import ch.pontius.swissqr.api.handlers.qr.generateQRCodeSimple
import ch.pontius.swissqr.api.handlers.qr.scanQRCode
import ch.pontius.swissqr.api.model.config.Config
import ch.pontius.swissqr.api.model.service.status.ErrorStatus
import ch.pontius.swissqr.api.model.service.status.ErrorStatusException
import ch.pontius.swissqr.api.utilities.KotlinxJsonMapper
import io.javalin.Javalin
import io.javalin.apibuilder.ApiBuilder.*
import io.javalin.openapi.OpenApiInfo
import io.javalin.openapi.plugin.DefinitionConfiguration
import io.javalin.openapi.plugin.OpenApiPlugin
import io.javalin.openapi.plugin.OpenApiPluginConfiguration
import io.javalin.openapi.plugin.SecurityComponentConfiguration
import io.javalin.openapi.plugin.swagger.SwaggerConfiguration
import io.javalin.openapi.plugin.swagger.SwaggerPlugin
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.Paths

/**
 * Entry point for Swiss QR API Service.
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
fun main(args: Array<String>) {

    val requestLogger = LoggerFactory.getLogger("ch.pontius.swissqr.api.requests")

    /* Load config file and start service. */
    val configPath = Paths.get(args.firstOrNull() ?: "./config.json")
    val config = Files.newBufferedReader(configPath).use { reader ->
        kotlinx.serialization.json.Json.decodeFromString(Config.serializer(), reader.readText())
    }

    /* Prepare Log4j logging facilities. */
    if (config.logConfig != null && Files.isRegularFile(config.logConfig)) {
        System.getProperties().setProperty("log4j.configurationFile", config.logConfig.toString())
    }

    Javalin.create { c ->
        /* Configure routes. */
        c.router.apiBuilder {
            path("api") {
                path("qr") {
                    path("generate") {
                        get("{type}") { generateQRCodeSimple(it) }
                        post("{type}") { generateQRCode(it) }
                    }
                    path("scan") {
                        post { scanQRCode(it) }
                    }
                }
            }
        }

        /* Enable CORS. */
        c.bundledPlugins.enableCors { cors ->
            cors.addRule {
                it.reflectClientOrigin =
                    true // anyHost() has similar implications and might be used in production? I'm not sure how to cope with production and dev here simultaneously
                it.allowCredentials = true
            }
        }

        /* We use Kotlinx serialization for de-/serialization. */
        c.jsonMapper(KotlinxJsonMapper)

        /* Registers Open API plugin. */
        c.registerPlugin(OpenApiPlugin { openApiConfig: OpenApiPluginConfiguration ->
            openApiConfig
                .withDocumentationPath("/swagger-docs")
                .withDefinitionConfiguration { _: String, openApiDefinition: DefinitionConfiguration ->
                    openApiDefinition
                        .withInfo { openApiInfo: OpenApiInfo ->
                            openApiInfo
                                .title("Swiss QR Web Service")
                                .version("1.0.0")
                                .description("This is Open API web service that can be used to generate and read Swiss QR codes for invoices.\n\nUsage requires a valid API token!")
                                .contact("pontius software GmbH", "https://www.pontius.ch", "rg@pontius.ch")
                        }
                        .withSecurity { openApiSecurity: SecurityComponentConfiguration ->
                            openApiSecurity.withBearerAuth("bearerAuth")
                        }
                }
        })

        /* Registers Swagger Plugin. */
        c.registerPlugin(SwaggerPlugin { swaggerConfiguration: SwaggerConfiguration ->
            swaggerConfiguration.documentationPath = "/swagger-docs"
            swaggerConfiguration.uiPath = "/swagger-ui"
        })
    }.beforeMatched { ctx ->
        /* Tries to obtain the access token. */
        if (ctx.path().startsWith("/api/")) {
            val tokenId = ctx.header(API_KEY_HEADER)?.replace("Bearer ", "") ?: ctx.queryParam(API_KEY_PARAM)
            if (tokenId == null) {
                throw ErrorStatusException(401, "API token is missing from request.")
            }
            if (!config.keys.contains(tokenId)) {
                throw ErrorStatusException(401, "API token is invalid.")
            }
        }
    }.before { ctx ->
        val req = ctx.req()
        requestLogger.debug("${req.method} request to ${ctx.path()} with params (${ctx.queryParamMap().map { e -> "${e.key}=${e.value}" }.joinToString()}) from ${req.remoteAddr}")
    }.error(401) { ctx ->
        ctx.json(ErrorStatus(401, "Unauthorized request!"))
        requestLogger.error("Unauthorized request to {}.", ctx.path())
    }.exception(ErrorStatusException::class.java) { e, ctx ->
        ctx.status(e.status).json(ErrorStatus(e.status, e.description))
        requestLogger.error("Exception during handling of request to {}: {}", ctx.path(), e)
    }.exception(Exception::class.java) { e, ctx ->
        ctx.status(500).json(ErrorStatus(500, "Internal server error: ${e.message}"))
        requestLogger.error("Exception during handling of request to {}: {}", ctx.path(), e)
    }.start(config.server.port)

    /* Wait for user to abort. */
    while (true) {
        Thread.sleep(1000)
    }
}