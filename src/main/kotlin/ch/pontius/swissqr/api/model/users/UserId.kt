package ch.pontius.swissqr.api.model.users

import ch.pontius.swissqr.api.model.Id
import java.util.*

/**
 * An [Id] for a [User] object.
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
data class UserId(override val value: String): Id {

    /**
     * Constructor for new [UserId].
     */
    constructor(): this(UUID.randomUUID().toString())
}