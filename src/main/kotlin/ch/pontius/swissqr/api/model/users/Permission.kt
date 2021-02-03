package ch.pontius.swissqr.api.model.users

import io.javalin.core.security.Role

/**
 * [Permission] a [User] or [Token] can have within this Swiss QR API Service.
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
enum class Permission(val public: Boolean): Role {
    QR_CREATE(true), /** This [Permission] has the ability to generate QR codes. */
    QR_SCAN(true), /** This [Permission] has the ability to scan QR codes. */
    ADMIN(false) /** This [Permission] has the ability to perform administrative duties such as creating users. */
}