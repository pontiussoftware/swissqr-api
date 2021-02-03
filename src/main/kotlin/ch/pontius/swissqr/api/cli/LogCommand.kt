package ch.pontius.swissqr.api.cli

import ch.pontius.swissqr.api.db.ListStore
import ch.pontius.swissqr.api.model.access.Access
import ch.pontius.swissqr.api.model.users.TokenId
import ch.pontius.swissqr.api.model.users.User
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.NoOpCliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.jakewharton.picnic.Table
import com.jakewharton.picnic.table

/**
 * A collection of [CliktCommand]s to query and manipulate the [Access] logs.
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
class LogCommand(private val logStore: ListStore<Access>) : NoOpCliktCommand(name = "log") {

    init {
        this.subcommands(ListLogsCommand())
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
    private fun tabulateAccessLogs(users: Iterable<Access?>): Table = table {
        cellStyle {
            border = true
            paddingLeft = 1
            paddingRight = 1
        }
        header {
            row("tokenId", "source", "path", "method", "status", "timestamp")
        }
        body {
            users.forEach {
                if (it != null) {
                    row(it.tokenId, it.ip, it.path, it.method, it.status, it.timestamp)
                }
            }
        }
    }

    /**
     * [CliktCommand] to list all available [User]s.
     */
    inner class ListLogsCommand : CliktCommand(name = "list", help = "Lists all access log entries.") {

        /** The [TokenId] to invalidate. */
        private val tokenId: TokenId? by option("-i", "--id").convert { TokenId(it) }

        /** Flag that can be set to only list active tokens. */
        private val status: Int? by option("-s", "--status", help = "Only lists entries that match the given status.")
            .convert { it.toInt() }

        /** Flag that can be set to only list active tokens. */
        private val after: Long by option("-a", "--after", help = "Only lists entries that happened after the given timestamp.")
            .convert { it.toLong() }
            .default(Long.MIN_VALUE)

        /** Flag that can be set to only list active tokens. */
        private val before: Long by option("-b", "--before", help = "Only lists entries that happened before the given timestamp.")
            .convert { it.toLong() }
            .default(Long.MAX_VALUE)

        /** Flag that can be set to only list active tokens. */
        private val limit: Int by option("-l", "--limit", help = "Only lists entries that happened before the given timestamp.")
            .convert { it.toInt() }
            .default(100)

        override fun run() {
            val logs = this@LogCommand.logStore.filter {
                var match = false
                if (it != null) {
                    match = (it.timestamp > this.after && it.timestamp < this.before)
                    if (this.status != null) {
                        match = match && (it.status == this.status)
                    }
                    if (this.status != null) {
                        match = match && (it.status == this.status)
                    }
                    if (this.tokenId != null) {
                        match = match && (it.tokenId == this.tokenId)
                    }
                }
                match
            }.takeLast(this.limit)
            println(this@LogCommand.tabulateAccessLogs(logs))
        }
    }
}