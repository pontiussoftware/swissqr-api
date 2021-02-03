package ch.pontius.swissqr.api.handlers.users

import ch.pontius.swissqr.api.basics.GetRestHandler
import ch.pontius.swissqr.api.db.DAO
import ch.pontius.swissqr.api.model.service.status.ErrorStatusException
import ch.pontius.swissqr.api.model.service.status.Status
import ch.pontius.swissqr.api.model.users.User
import ch.pontius.swissqr.api.model.users.UserId

import io.javalin.http.Context
import io.javalin.plugin.openapi.annotations.*

/**
 * Handler that can be used to confirm a [User] that was freshly created.
 *
 * @author Ralph Gasser
 * @version 1.0
 */
class ConfirmUserHandler(val dao: DAO<User>): GetRestHandler {
    override val route: String = "user/confirm/:user/:nonce"

    @OpenApi(
        summary = "Confirms a newly created user.",
        path = "/api/user/confirm/:user/:nonce", method = HttpMethod.GET,
        tags = ["User management"],
        pathParams = [
            OpenApiParam("user", String::class, "ID of the user that should be activated."),
            OpenApiParam("nonce", Long::class, "Nonce, which is checksum of the user's personal details. Makes sure that only eligible people can activate a user.")
        ],
        responses = [
            OpenApiResponse("200", [OpenApiContent(Status.SuccessStatus::class)]),
            OpenApiResponse("400", [OpenApiContent(Status.ErrorStatus::class)]),
            OpenApiResponse("404", [OpenApiContent(Status.ErrorStatus::class)])
        ]
    )
    override fun doGet(ctx: Context) {
        /* Extract relevant parameters. */
        val userid =  ctx.pathParamMap()["user"] ?: throw ErrorStatusException(400, "User ID is missing from the request or invalid!")
        val nonce =  ctx.pathParamMap()["nonce"]?.toLongOrNull() ?: throw ErrorStatusException(400, "Nonce is missing from the request or invalid!")

        /* Prepare new user object. */
        val user = this.dao[UserId(userid)] ?: throw ErrorStatusException(404, "Requested user cannot be confirmed.")
        if (user.confirmed) {
            throw ErrorStatusException(400, "Requested user cannot be confirmed; user has been confirmed already.")
        }

        /* Check nonce. */
        if (nonce != user.nonce()) {
            throw ErrorStatusException(400, "Requested user cannot be confirmed; nonce invalid.")
        }

        /* Confirm user and store it. */
        user.confirmed = true
        this.dao.update(user)

        /* Write success response. */
        ctx.json(Status.SuccessStatus("User '${user.email}' created successfully."))
    }
}