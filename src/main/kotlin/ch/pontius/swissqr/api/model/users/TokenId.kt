package ch.pontius.swissqr.api.model.users

import ch.pontius.swissqr.api.model.Id
import java.util.*

/**
 * An [Id] for a [Token] object.
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
class TokenId(override val value: String): Id {

    /**
     * Constructor for new [UserId].
     */
    constructor(): this(UUID.randomUUID().toString())
}