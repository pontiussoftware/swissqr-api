package ch.pontius.swissqr.api.cli

import ch.pontius.swissqr.api.db.MapStore
import ch.pontius.swissqr.api.model.users.Password
import ch.pontius.swissqr.api.model.users.Token
import ch.pontius.swissqr.api.model.users.User
import ch.pontius.swissqr.api.model.users.UserId

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.NoOpCliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.options.*
import com.jakewharton.picnic.Table
import com.jakewharton.picnic.table

import kotlin.system.measureTimeMillis

/**
 * A collection of [CliktCommand]s for [User] management
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
class UserCommand(val userStore: MapStore<User>, val tokenStore: MapStore<Token>) : NoOpCliktCommand(name = "user") {

    companion object {
        val EMAIL_VALIDATION_REGEX = Regex("(?:[a-z0-9!#\$%&'*+/=?^_`{|}~-]+(?:\\.[a-z0-9!#\$%&'*+/=?^_`{|}~-]+)*|\"(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21\\x23-\\x5b\\x5d-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])*\")@(?:(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?|\\[(?:(?:(2(5[0-5]|[0-4][0-9])|1[0-9][0-9]|[1-9]?[0-9]))\\.){3}(?:(2(5[0-5]|[0-4][0-9])|1[0-9][0-9]|[1-9]?[0-9])|[a-z0-9-]*[a-z0-9]:(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21-\\x5a\\x53-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])+)\\])")
    }

    init {
        this.subcommands(
            CreateUserCommand(),
            InvalidateUserCommand(),
            UpdateUserCommand(),
            ListUsersCommand()
        )
    }

    /** List of defined aliases for this [UserCommand]. */
    override fun aliases(): Map<String, List<String>> {
        return mapOf(
            "ls" to listOf("list"),
            "remove" to listOf("delete"),
            "drop" to listOf("delete"),
            "add" to listOf("create")
        )
    }

    /**
     * Tabulates the given [Iterable] of [User] objects.
     *
     * @param users [Iterable] of [User] objects
     * @return Resulting [Table]
     */
    private fun tabulateUsers(users: Iterable<User?>): Table = table {
        cellStyle {
            border = true
            paddingLeft = 1
            paddingRight = 1
        }
        header {
            row("id", "email", "password", "active", "confirmed", "description")
        }
        body {
            users.forEach {
                if (it != null) {
                    row(it.id, it.email, it.password, it.active, it.confirmed, it.description)
                }
            }
        }
    }

    /**
     * [CliktCommand] to create a new [User].
     */
    inner class CreateUserCommand : CliktCommand(name = "create", help = "Creates a new user for Swiss QR API.") {
        private val email: String by option("-e", "--email", help = "A valid email address.")
            .required()
            .validate {
                if (!it.matches(EMAIL_VALIDATION_REGEX)) {
                    fail("You must provide a valid e-mail address.")
                }
            }

        private val password: Password.PlainPassword by option("-p", "--password", help = "Password of at least ${Password.MIN_PASSWORD_LENGTH} characters.")
            .convert { Password.PlainPassword(it) }
            .required()
            .validate {
                if (it.value.length < Password.MIN_PASSWORD_LENGTH) {
                    fail("Password must consist of at least ${Password.MIN_PASSWORD_LENGTH} characters.")
                }
            }

        private val description: String? by option("-d", "--description", help = "User account description.")

        override fun run() {
            val user = User(email = this.email, password = this.password, description = description, active = true, confirmed = true)
            val duration = measureTimeMillis {
                this@UserCommand.userStore.update(user)
            }
            println("New user created successfully (took ${duration}ms):")
            println(this@UserCommand.tabulateUsers(listOf(user)))
        }
    }

    /**
     * [CliktCommand] to create a new [User].
     */
    inner class InvalidateUserCommand : CliktCommand(name = "invalidate", help = "Invalidates a user for Swiss QR API and all tokens associated with it.") {
        private val userId: UserId by option("-i", "--id").convert { UserId(it) }.required()
        override fun run() {
            val user = this@UserCommand.userStore[this.userId] ?: throw IllegalArgumentException("Failed to invalidate user: User $userId does not exist.")
            val duration = measureTimeMillis {
                this@UserCommand.userStore.update(user.copy(active = false))
                this@UserCommand.tokenStore.forEach {
                    if (it?.userId == user.id && it.active) {
                        this@UserCommand.tokenStore.update(it.copy(active = false))
                    }
                }
            }
            println("User invalidated successfully (took ${duration}ms):")
            println(this@UserCommand.tabulateUsers(listOf(user)))
        }
    }

    /**
     * [CliktCommand] to update an existing [User].
     */
    inner class UpdateUserCommand : CliktCommand(name = "update", help = "Updates properties of a user.") {
        private val id: UserId by option("-i", "--id").convert { UserId(it) }.required()

        private val email: String? by option("-e", "--email", help = "A valid email address.")
            .validate {
                if (!it.matches(EMAIL_VALIDATION_REGEX)) {
                    fail("You must provide a valid e-mail address.")
                }
            }

        private val password: Password.PlainPassword? by option("-p", "--password", help = "Password of at least ${Password.MIN_PASSWORD_LENGTH} characters.")
            .convert { Password.PlainPassword(it) }
            .validate {
                if (it.value.length < Password.MIN_PASSWORD_LENGTH) {
                    fail("Password must consist of at least ${Password.MIN_PASSWORD_LENGTH} characters.")
                }
            }

        private val description: String? by option("-d", "--description", help = "User account description.")

        override fun run() {
            val user = this@UserCommand.userStore[this.id]
            if (user != null) {
                val duration = measureTimeMillis {
                    this@UserCommand.userStore.update(user.copy(
                        email = this.email ?: user.email,
                        password = (this.password?.hash() ?: user.password),
                        description = (this.description ?: user.description)
                    ))
                }

                println("User updated successfully (took ${duration}ms):")
                println(this@UserCommand.tabulateUsers(listOf(user)))
            } else {
                println("User ${this.id} could not be found.")
            }
        }
    }

    /**
     * [CliktCommand] to list all available [User]s.
     */
    inner class ListUsersCommand : CliktCommand(name = "list", help = "Lists all users.") {
        /** Flag that can be set to only list active tokens. */
        private val activeOnly: Boolean by option("-a", "--active", help = "Only lists active users.")
            .convert { it.toBoolean() }
            .default(false)

        override fun run() {
            if (!this.activeOnly) {
                println("Available users: ${this@UserCommand.userStore.size}")
                println(this@UserCommand.tabulateUsers(this@UserCommand.userStore))
            } else {
                val active = this@UserCommand.userStore.filterNot { it != null && it.active }
                println("Available users: ${active.size}")
                println(this@UserCommand.tabulateUsers(active))
            }
        }
    }
}
