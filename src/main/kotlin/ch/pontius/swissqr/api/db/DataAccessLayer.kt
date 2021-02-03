package ch.pontius.swissqr.api.db

import ch.pontius.swissqr.api.model.access.Access
import ch.pontius.swissqr.api.model.users.Token
import ch.pontius.swissqr.api.model.users.User
import java.nio.file.Path

/**
 * The data access layer used by Swiss QR Service.
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
class DataAccessLayer(private val basePath: Path) {

    /** Store for [User] objects registered with this [DataAccessLayer].*/
    val userStore: MapStore<User> = MapStore(this.basePath.resolve("users.db"), User.Serializer)

    /** Store for [Token] objects registered with this [DataAccessLayer].*/
    val tokenStore: MapStore<Token> = MapStore(this.basePath.resolve("tokens.db"), Token.Serializer)

    /** Log of [Access] entries for this instance of Swiss QR API.*/
    val accessLogs: ListStore<Access> = ListStore(this.basePath.resolve("logs.db"), Access.Serializer)

}