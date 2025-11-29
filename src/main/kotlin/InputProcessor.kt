import commands.CommandType
import commands.getCommand

var lastFullInput = ""

fun readLine(line: String?) = line?.split("&")?.forEach { readCommand(it) }

fun readCommand(line: String) {
    val parts = line.parseArgs().map { it.lowercase() }
    lastFullInput = line
    when {
        parts.isEmpty() -> CommandType.HELP.usage
        parts.size == 1 && parts.first().startsWith("nxm://") -> addModByNexusProtocol(parts.first())
        else -> {
            val commandString = parts.first().lowercase()
            val args = parts.subList(1, parts.size)

            val command = getCommand(commandString)
            if (command != null) {
                command.apply(commandString, args)
            } else {
                println("Unknown Command $commandString")
            }
        }
    }
}

fun String.parseArgs(home: String = HOME): List<String> {
    return replace("~/", "$home/").split("\"").chunked(2).flatMap { parts ->
        val second = if (parts.size == 2) listOf(parts.last()) else listOf()
        parts.first().trim().split(" ") + second
    }.filter { it.isNotBlank() }
}

fun String.urlToId(): Int? {
    return replace("https://www.nexusmods.com/starfield/mods/", "").let { idPart ->
        val end = idPart.indexOf("?").takeIf { it > 0 } ?: idPart.length
        idPart.substring(0, end)
    }.toIntOrNull().also {
        if (it == null) println("Could not find id in $this")
    }
}
