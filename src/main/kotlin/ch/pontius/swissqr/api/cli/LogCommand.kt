package ch.pontius.swissqr.api.cli

import ch.pontius.swissqr.api.db.ListStore
import ch.pontius.swissqr.api.model.access.Access
import ch.pontius.swissqr.api.model.users.Token
import ch.pontius.swissqr.api.model.users.TokenId
import ch.pontius.swissqr.api.model.users.User
import ch.pontius.swissqr.api.model.users.UserId
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.NoOpCliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.jakewharton.picnic.Table
import com.jakewharton.picnic.table
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import java.text.SimpleDateFormat
import java.util.*

/**
 * A collection of [CliktCommand]s to query and manipulate the [Access] logs.
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
class LogCommand(private val logStore: ListStore<Access>) : NoOpCliktCommand(name = "log") {

    init {
        this.subcommands(SummaryCommand(), ListLogsCommand())
    }

    /** List of defined aliases for this [UserCommand]. */
    override fun aliases(): Map<String, List<String>> = mapOf(
        "ls" to listOf("list")
    )

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
     * Tabulates the given [Iterable] of [User] objects.
     *
     * @param users [Iterable] of [User] objects
     * @return Resulting [Table]
     */
    private fun exportAccessLogs(out: Path, users: Iterable<Access?>) = Files.newBufferedWriter(out, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE).use {
        it.write("tokenId,source,path,method,status,timestamp")
        it.newLine()
        for (user in users) {
            if (user != null) {
                it.write("${user.tokenId},${user.ip},${user.path},${user.method},${user.status},${user.timestamp}")
            }
            it.newLine()
        }
        it.flush()
    }

    /**
     * [CliktCommand] to summarize [Access] logs.
     */
    inner class SummaryCommand : CliktCommand(name = "summary", help = "Creates a summary of all the logs.") {
        /** Flag that can be set to only list active tokens. */
        private val status: Int? by option("-s", "--status", help = "Only lists entries that match the given status.")
            .convert { it.toInt() }

        /** Flag that can be set to only list active tokens. */
        private val after: Long by option("-a", "--after", help = "Only lists entries that happened after the given date (YYYY-MM-DD).")
            .convert { SimpleDateFormat("yyyy-mm-dd").parse(it).time }
            .default(Long.MIN_VALUE)

        /** Flag that can be set to only list active tokens. */
        private val before: Long by option("-b", "--before", help = "Only lists entries that happened before the given date (YYYY-MM-DD).")
            .convert { it.toLong() }
            .default(Long.MAX_VALUE)

        /** Flag that can be set to only list active tokens. */
        private val output: Path? by option("-o", "--out", help = "Routes the output of the command into the given output file.")
            .convert { Paths.get(it) }

        override fun run() {
            val summary = mutableMapOf<TokenId,Long>()
            val logs = this@LogCommand.logStore.filterNotNull().filter {
                var match = (it.timestamp > this.after && it.timestamp < this.before)
                if (this.status != null) {
                    match = match && (it.status == this.status)
                }
                match
            }.forEach {
                summary.compute(it.tokenId) { k, v ->
                    val res = v ?: 0L
                    res + 1
                }
            }

            /* Print or export logs. */
            if (this.output == null) {
                println(table {
                    cellStyle {
                        border = true
                        paddingLeft = 1
                        paddingRight = 1
                    }
                    header {
                        row("tokenId", "accesses")
                    }
                    body {
                        summary.forEach {
                            row(it.key, it.value)
                        }
                    }
                })
            } else {
                Files.newBufferedWriter(this.output!!, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE).use { writer ->
                    writer.write("tokenId,accesses")
                    writer.newLine()
                    summary.forEach {
                        writer.write("${it.key},${it.value}")
                        writer.newLine()
                    }
                    writer.flush()
                }
                println("${summary.size} summaries exported to ${this.output}")
            }
        }
    }

    /**
     * [CliktCommand] to list all available [Access] logs.
     */
    inner class ListLogsCommand : CliktCommand(name = "list", help = "Lists all access log entries.") {

        /** The [TokenId] to invalidate. */
        private val tokenId: TokenId? by option("-i", "--id").convert { TokenId(it) }

        /** Flag that can be set to only list active tokens. */
        private val status: Int? by option("-s", "--status", help = "Only lists entries that match the given status.")
            .convert { it.toInt() }

        /** Flag that can be set to only list active tokens. */
        private val after: Long by option("-a", "--after", help = "Only lists entries that happened after the given date (YYYY-MM-DD).")
            .convert { SimpleDateFormat("yyyy-mm-dd").parse(it).time }
            .default(Long.MIN_VALUE)

        /** Flag that can be set to only list active tokens. */
        private val before: Long by option("-b", "--before", help = "Only lists entries that happened before the given date (YYYY-MM-DD).")
            .convert { it.toLong() }
            .default(Long.MAX_VALUE)

        /** Flag that can be set to only list active tokens. */
        private val limit: Int by option("-l", "--limit", help = "Limits the result set to the given number of entries (default = 100).")
            .convert { it.toInt() }
            .default(100)

        /** Flag that can be set to only list active tokens. */
        private val output: Path? by option("-o", "--out", help = "Routes the output of the command into the given output file.")
            .convert { Paths.get(it) }

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

            /* Print or export logs. */
            if (this.output == null) {
                println(this@LogCommand.tabulateAccessLogs(logs))
            } else {
                this@LogCommand.exportAccessLogs(this.output!!, logs)
                println("${logs.size} log entries exported to ${this.output}")
            }
        }
    }
}