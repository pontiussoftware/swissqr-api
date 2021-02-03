package ch.pontius.swissqr.api.handlers.users

import ch.pontius.swissqr.api.basics.PostRestHandler
import ch.pontius.swissqr.api.db.DAO
import ch.pontius.swissqr.api.model.service.status.ErrorStatusException
import ch.pontius.swissqr.api.model.service.status.Status
import ch.pontius.swissqr.api.model.service.user.UserRequest
import ch.pontius.swissqr.api.model.users.Role
import ch.pontius.swissqr.api.model.users.User
import io.javalin.http.BadRequestResponse
import io.javalin.http.Context
import io.javalin.plugin.openapi.annotations.*
import javax.mail.Flags.Flag.USER

/**
 * Handler that can be used to create a [User].
 *
 * @author Ralph Gasser
 * @version 1.0
 */
class CreateUserHandler(val dao: DAO<User>): PostRestHandler {
    override val route: String = "user/create"

    @OpenApi(
        summary = "Creates a new user.",
        path = "/api/user/create", method = HttpMethod.POST,
        requestBody = OpenApiRequestBody([OpenApiContent(from = UserRequest::class)]),
        tags = ["User management"],
        responses = [
            OpenApiResponse("200", [OpenApiContent(Status.SuccessStatus::class)]),
            OpenApiResponse("400", [OpenApiContent(Status.ErrorStatus::class)]),
            OpenApiResponse("401", [OpenApiContent(Status.ErrorStatus::class)])
        ]
    )
    override fun doPost(ctx: Context) {
        val request = try {
            ctx.bodyAsClass(UserRequest::class.java)
        } catch (e: BadRequestResponse){
            throw ErrorStatusException(400, "HTTP body could not be parsed into a valid user request!")
        }

        /* Prepare new user object. */
        val user = User(email = request.email, password = request.password, active = true, confirmed = false)
        this.dao.update(user)

        /* Write success response. */
        ctx.json(Status.SuccessStatus("User '${user.email}' created successfully."))
    }
}