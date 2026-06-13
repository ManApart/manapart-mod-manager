package commands.view

import Mod
import doCommand
import toolData

val bufferDescription = """
    Manipulate a buffer that other commands can use.
    The buffer is in memory only and does not persist across restarts
    buffer ls shows just the mod indices. You can use ls id or ls all to see the ids of the mods or the whole mod
    You can add or remove a mod to the buffer using it's index
    You can set the buffer to a list of indicies, or use buffer set id to set it to a list based on id
""".trimIndent()

val bufferUsage = """
    buffer ls
    buffer ls id
    buffer ls all
    buffer add 1
    buffer add id 1
    buffer rm 2
    buffer rm id 2
    buffer set 1 2 3
    buffer set id 1 2 3
""".trimIndent()

var BUFFER = setOf<Mod>()

fun buffer(command: String, args: List<String> = listOf()) {
    if (args.isEmpty()) {
        println(bufferDescription)
        return
    }
    val rest = args.drop(1)
    when (args.first()) {
        "ls" -> listBuffer(args)
        "add" -> add(rest)
        "rm" -> remove(rest)
        "set" -> set(rest)
        else -> println(bufferDescription)
    }
}

private fun listBuffer(args: List<String>) {
    if (BUFFER.isEmpty()) {
        println("Buffer is empty")
        return
    }
    when (args.last()) {
        "id" -> println(BUFFER.joinToString(" ") { "" + it.id })
        "all" -> display(BUFFER.toList())
        else -> println(BUFFER.joinToString(" ") { "" + it.index })
    }
}

private fun add(args: List<String>) {
    when {
        args.isEmpty() -> println("Must supply mods to add")
        args.first() == "id" -> BUFFER = BUFFER + args.idsToMods()
        else -> doCommand(args.drop(1)) { (BUFFER + this).setMods() }
    }
}

private fun remove(args: List<String>) {
    when {
        args.isEmpty() -> println("Must supply mods to remove")
        args.first() == "id" -> BUFFER = BUFFER - args.idsToMods().toSet()
        else -> doCommand(args.drop(1)) { (BUFFER - toSet()).setMods() }
    }
}

private fun set(args: List<String>) {
    when {
        args.isEmpty() -> println("Must supply mods to set buffer to")
        args.first() == "id" -> BUFFER = args.idsToMods()
        else -> doCommand(args.drop(1)) { toSet().setMods() }
    }
}

private fun List<String>.idsToMods() = drop(1).mapNotNull { it.toIntOrNull() }.mapNotNull { toolData.byId(it) }.toSet()

private fun Set<Mod>.setMods() {
    BUFFER = this
}
