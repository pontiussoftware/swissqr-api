package ch.pontius.swissqr.api.cli

import ch.pontius.swissqr.api.db.DataAccessLayer
import ch.pontius.swissqr.api.model.config.Config
import com.github.ajalt.clikt.core.NoOpCliktCommand
import com.github.ajalt.clikt.core.context
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.output.CliktHelpFormatter
import com.github.ajalt.clikt.output.HelpFormatter
import org.jline.reader.LineReaderBuilder
import org.jline.terminal.Terminal
import org.jline.terminal.TerminalBuilder
import java.io.IOException
import java.util.ArrayList
import java.util.regex.Pattern
import kotlin.system.exitProcess

/**
 * The [Cli] class that provides basic CLI functionality.
 *
 * @author Ralph Gasser
 * @version 1.0.0
 */
class Cli(val dataAccessLayer: DataAccessLayer, val config: Config) {

    /** Prompt for CLI. */
    companion object {
        private const val PROMPT = "QR> "
    }

    /** Base [NoOpCliktCommand] with list of all knonw subcommands. */
    private val clikt = object: NoOpCliktCommand(name = "qr"){
        init {
            context { helpFormatter = CliHelpFormatter()}
        }
    }.subcommands(
        UserCommand()
    )

    /**
     * Starts the CLI loop; this method blocks until user types `exit` or `quit` upon which the
     * program will terminate.
     */
    fun loop() {
        var terminal: Terminal? = null
        try {
            terminal = TerminalBuilder.terminal() //basic terminal
        } catch (e: IOException) {
            System.err.println("Could not initialize Terminal: ")
            System.err.println(e.message)
            System.err.println("Exiting...")
            exitProcess(-1)
        }

        val lineReader = LineReaderBuilder.builder().terminal(terminal).build()

        while (true) {
            val line = lineReader.readLine(PROMPT).trim()
            if (line.toLowerCase() == "exit" || line.toLowerCase() == "quit") {
                break
            }
            if (line.toLowerCase() == "help") {
                println(clikt.getFormattedHelp()) //TODO overwrite with something more useful in a cli context
                continue
            }
            if (line.isBlank()){
                println("Please enter a valid command; type help for list of commands.")
                continue
            }

            try {
                clikt.parse(splitLine(line))
            } catch (e: Exception) {
                when (e) {
                    is com.github.ajalt.clikt.core.NoSuchSubcommand -> println("Please enter a valid command; type help for list of commands.")
                    is com.github.ajalt.clikt.core.PrintHelpMessage -> println(e.command.getFormattedHelp())
                    is com.github.ajalt.clikt.core.MissingParameter -> println(e.localizedMessage)
                    is com.github.ajalt.clikt.core.NoSuchOption -> println(e.localizedMessage)
                    else -> e.printStackTrace()
                }
            }
        }
    }

    val lineSplitRegex: Pattern = Pattern.compile("[^\\s\"']+|\"([^\"]*)\"|'([^']*)'")

    //based on https://stackoverflow.com/questions/366202/regex-for-splitting-a-string-using-space-when-not-surrounded-by-single-or-double/366532
    private fun splitLine(line: String?): List<String> {
        if (line == null || line.isEmpty()) {
            return emptyList()
        }
        val matchList: MutableList<String> = ArrayList()
        val regexMatcher = lineSplitRegex.matcher(line)
        while (regexMatcher.find()) {
            if (regexMatcher.group(1) != null) {
                // Add double-quoted string without the quotes
                matchList.add(regexMatcher.group(1))
            } else if (regexMatcher.group(2) != null) {
                // Add single-quoted string without the quotes
                matchList.add(regexMatcher.group(2))
            } else {
                // Add unquoted word
                matchList.add(regexMatcher.group())
            }
        }
        return matchList
    }

    class CliHelpFormatter : CliktHelpFormatter() {
        override fun formatHelp(
            prolog: String,
            epilog: String,
            parameters: List<HelpFormatter.ParameterHelp>,
            programName: String
        ) = buildString {
            addOptions(parameters)
            addArguments(parameters)
            addCommands(parameters)
        }
    }
}