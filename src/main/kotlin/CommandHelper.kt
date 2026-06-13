import commands.getIndicesOrRange
import commands.view.BUFFER
import java.io.File

fun doCommand(args: List<String>, execute: List<Mod>.() -> Unit) {
    if (args.isEmpty()) {
        toolData.mods.execute()
        return
    }
    when(args.first()) {
        "all" -> toolData.mods.execute()
        "empty" -> executeFiltered(execute) { !File(it.filePath).exists() }
        "staged" -> executeFiltered(execute) { File(it.filePath).exists() }
        "enabled" -> executeFiltered(execute) { it.enabled }
        "disabled" -> executeFiltered(execute) { !it.enabled }
        in "buffer", "bf" -> BUFFER.toList().execute()
        else -> {
            args.getIndicesOrRange(toolData.mods.size)
                .mapNotNull { toolData.byIndex(it) }
                .execute()
        }
    }
}

private fun executeFiltered(execute: List<Mod>.() -> Unit, filter: (Mod) -> Boolean) {
    toolData.mods.filter(filter).execute()
}
