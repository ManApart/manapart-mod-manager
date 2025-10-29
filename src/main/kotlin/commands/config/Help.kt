package commands.config

import Column
import Table
import capitalize
import commands.CommandType
import commands.getCommand
import cyan

const val helpDescription = "List the commands for view a specific command in detail"
val helpUsage = """
    help
    help <command>
""".trimIndent()

fun help(command: String, args: List<String> = listOf()) {
    val helpCommand = args.firstOrNull()?.let { getCommand(it) }
    when {
        args.isEmpty() -> printGeneralHelp()
        args.first() == "all" -> printDetailedHelp()
        helpCommand != null -> {
            println("Aliases: " + helpCommand.aliases.joinToString(", ") + "\n")
            println(helpCommand.description + "\n")
            println(helpCommand.usage)
        }

        else -> printGeneralHelp()
    }
}

private fun printGeneralHelp() {
    CommandType.entries
        .filterNot { it == CommandType.HELP }
        .groupBy { it.category }.entries
        .forEach { (category, commands) ->
            println(cyan(category.name.lowercase().capitalize()))
            val columns = listOf(
                Column("Command", 15),
                Column("Aliases", 20),
                Column("Summary", 30),
            )
            val data = commands.map {
                mapOf(
                    "Command" to it.cleanName,
                    "Aliases" to it.aliases.joinToString(", "),
                    "Summary" to it.summary,
                )
            }
            Table(columns, data).print(false)
            println()
        }
}

private fun printDetailedHelp() {
    println(CommandType.entries.filterNot { it == CommandType.HELP }.joinToString("\n\n") {
       it.detailedHelp()
    })
}

private fun CommandType.detailedHelp(): String{
    val descriptionString = description.replace("\n", "\n\t")
    val usageString = usage.replace("\n", "\n\t")
    return "${cyan(cleanName)}: ${summary}\n\t$descriptionString\n\n\t$usageString"
}
