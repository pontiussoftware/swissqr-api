package ch.pontius.swissqr.api.cli

import ch.pontius.swissqr.api.db.MapStore
import ch.pontius.swissqr.api.model.users.*
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.NoOpCliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.options.*
import com.jakewharton.picnic.Table
import com.jakewharton.picnic.table
import kotlin.system.measureTimeMillis

/**
 * A collection of [CliktCommand]s for [Token] management
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
class TokenCommand(val tokenStore: MapStore<Token>, val userStore: MapStore<User>) : NoOpCliktCommand(name = "token") {

    init {
        this.subcommands(CreateTokenCommand(), InvalidateTokenCommand(), ListTokensCommand())
    }

    /** List of defined aliases for this [UserCommand]. */
    override fun aliases(): Map<String, List<String>> {
        return mapOf(
            "ls" to listOf("list"),
            "delete" to listOf("invalidate"),
            "remove" to listOf("invalidate"),
            "drop" to listOf("invalidate"),
            "add" to listOf("create")
        )
    }

    /**
     * Tabulates the given [Iterable] of [User] objects.
     *
     * @param users [Iterable] of [User] objects
     * @return Resulting [Table]
     */
    private fun tabulateTokens(users: Iterable<Token?>): Table = table {
        cellStyle {
            border = true
            paddingLeft = 1
            paddingRight = 1
        }
        header {
            row("id", "userId", "active", "created", "permissions")
        }
        body {
            users.forEach {
                if (it != null) {
                    row(it.id, it.userId, it.active, it.created, it.roles.joinToString(","))
                }
            }
        }
    }

    /**
     * [CliktCommand] to create new [Token]s.
     */
    inner class CreateTokenCommand : CliktCommand(name = "create", help = "Creates a new API token for Swiss QR API.") {

        /** The [UserId] to create the [Token] for. */
        private val userId: UserId by option("-i", "--id").convert { UserId(it) }.required()

        /** The [Permission]s the token should have. */
        private val roles: List<Permission> by option("-r", "--role").convert { Permission.valueOf(it) }.multiple()

        override fun run() {
            val user = this@TokenCommand.userStore[this.userId] ?: throw IllegalArgumentException("Failed to create token: User $userId does not exist.")
            val token = Token(user, this.roles.toTypedArray())
            val duration = measureTimeMillis {
                this@TokenCommand.tokenStore.update(token)
            }
            println("New token created successfully (took ${duration}ms):")
            println(this@TokenCommand.tabulateTokens(listOf(token)))
        }
    }

    /**
     * [CliktCommand] to invalidate [Token]s.
     */
    inner class InvalidateTokenCommand : CliktCommand(name = "invalidate", help = "Invalidates a API token for Swiss QR API.") {
        /** The [TokenId] to invalidate. */
        private val tokenId: TokenId by option("-i", "--id").convert { TokenId(it) }.required()

        override fun run() {
            val token = this@TokenCommand.tokenStore[this.tokenId] ?: throw IllegalArgumentException("Failed to invalidate token: Token $tokenId does not exist.")
            val duration = measureTimeMillis {
                this@TokenCommand.tokenStore.update(token.copy(active = false))
            }
            println("Token $tokenId invalidated successfully (took ${duration}ms):")
            println(this@TokenCommand.tabulateTokens(listOf(token)))
        }
    }

    /**
     * [CliktCommand] to list all available [User]s.
     */
    inner class ListTokensCommand : CliktCommand(name = "list", help = "Lists all available API tokens.") {

        /** Flag that can be set to only list active tokens. */
        private val activeOnly: Boolean by option("-a", "--active", help = "Only lists active tokens.")
            .convert { it.toBoolean() }
            .default(false)

        override fun run() {
            if (!this.activeOnly) {
                println("Available tokens: ${this@TokenCommand.userStore.size}")
                println(this@TokenCommand.tabulateTokens(this@TokenCommand.tokenStore))
            } else {
                val active = this@TokenCommand.tokenStore.filterNot { it != null && it.active }
                println("Available tokens: ${active.size}")
                println(this@TokenCommand.tabulateTokens(active))
            }
        }
    }
}