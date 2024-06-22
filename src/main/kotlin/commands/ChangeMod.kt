package commands

import addModFile
import save
import toolData
import java.io.File

val changeDescription = """
    Change various parts of a mod.
    Use id to set its nexus id
    Use file to delete a mods stage folder and restage it from zip
""".trimIndent()
val renameHelp = """
    Use name to rename a mod without changing its file paths
""".trimIndent()

val changeUsage = """
    mod <index> id <new id>
    mod <index> file <file-path> 
    mod <index> name <new name>
""".trimIndent()

fun moveMod(args: List<String>){
    changeMod(listOf(args.first(), "name") + args.drop(1))
}

fun changeMod(args: List<String>) {
    val i = args.firstOrNull()?.toIntOrNull()
    when {
        args.isEmpty() -> println(changeDescription)
        args.size == 3 && args[1] == "id" -> updateId(i!!, args.last().toInt())
        args.size == 3 && args[1] == "file" -> updateFile(i!!, args.last())
        args.size == 3 && args[1] == "name" -> updateName(i!!, args.last())
        else -> println(changeDescription)
    }
}

private fun updateId(i: Int, id: Int) {
    toolData.byIndex(i)?.let { mod ->
        println("${mod.name} id updated ${mod.id ?: "?"} -> $id")
        mod.id = id
        save()
    }
}

private fun updateName(i: Int, newName:String) {
    toolData.byIndex(i)?.let { mod ->
        println("${mod.name} renamed to $newName")
        mod.name = newName
        save()
    }
}

private fun updateFile(i: Int, sourceFilePath: String) {
    toolData.byIndex(i)?.let { mod ->
        val sourceFile = File(sourceFilePath)
        if (sourceFile.exists()) {
            val existing = File(mod.filePath)
            if (existing.exists()) existing.deleteRecursively()
            addModFile(mod, sourceFile, mod.name)
        } else {
            println("No file found at $sourceFilePath")
        }
    }
}