package commands.config

import logNote

val noteDescription = """
    Make a note in your log (used for stats)
    The note will be visible alongside other events in your log.jsonl
    This is useful for providing a reason, say before a big update to your mod list or a new playthrough
""".trimIndent()

val noteUsage = """
    note <text>
""".trimIndent()

fun noteCommand(command: String, args: List<String>) {
    logNote(args.joinToString(" "))
    println("Note saved")
}
