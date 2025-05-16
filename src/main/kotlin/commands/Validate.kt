package commands

import Mod
import StageChange
import cyan
import detectStagingChanges
import doCommand
import gameMode
import green
import red
import toolData
import yellow
import java.io.File

val validateDescription = """
    Examines mods for issues. Checks for duplicates, bad folder staging etc
    Use validate skip 1 to add a tag so that mod at index 1 is skipped for validation
    Use validate check 1 to remove the tag, so the mod is validated
""".trimIndent()

val validateUsage = """
    validate - defaults to validating enabled
    validate all
    validate <index>
    validate 1 2 4
    validate 1-3
    validate staged
    validate disabled
    validate skip 1
    validate check 1
""".trimIndent()

fun validateMods(command: String, args: List<String>) {
    val i = args.getOrNull(args.size - 1)?.toIntOrNull()
    when {
        args.isEmpty() -> toolData.mods.filter { it.enabled }.validate()
        args.first() == "skip" && i != null -> {
            toolData.byIndex(i)?.let {
                it.add(Tag.SKIP_VALIDATE)
                println("Skipping ${it.description()} during validation")
            }
        }

        args.first() == "check" && i != null -> {
            toolData.byIndex(i)?.let {
                it.remove(Tag.SKIP_VALIDATE)
                println("Considering ${it.description()} for validation")
            }
        }

        else -> doCommand(args, List<Mod>::validate)
    }
}

fun List<Mod>.validate() {
    val errorMap = mutableMapOf<Int, Pair<Mod, MutableList<String>>>()
    val nonModErrors = mutableListOf<String>()
    val helpMessages = mutableSetOf<String>()
    val modsWithFiles = associateWith { it.getModFiles() }

    addDupeIds(errorMap)
    addDupeFilenames(errorMap)
    detectStagingIssues(errorMap)
    detectDupePlugins()
    detectIncorrectCasing(errorMap)
    modsWithFiles.detectTopLevelFiles(errorMap)
    checkPlugins(errorMap, helpMessages)

    if (gameMode == GameMode.STARFIELD) {
        checkCreations(nonModErrors, helpMessages)
        checkExternalMods(nonModErrors, helpMessages)
    }

    val filteredErrors = errorMap.filter { it.value.first in this }.toMap()
    if (filteredErrors.isNotEmpty()) {
        printErrors(filteredErrors)
        println()
    }
    if (nonModErrors.isNotEmpty()) {
        nonModErrors.forEach { println(it) }
        println()
    }
    if (helpMessages.isNotEmpty()) {
        helpMessages.forEach { println(it) }
        println()
    }
    println(cyan("Validated $size mods"))
}

private fun List<Mod>.addDupeIds(
    errorMap: MutableMap<Int, Pair<Mod, MutableList<String>>>
) {
    groupBy { it.id }.filter { it.key != null && it.value.size > 1 }.map { it.value }.forEach { dupes ->
        val indexes = dupes.map { it.index }
        dupes.forEach { dupe ->
            errorMap.putIfAbsent(dupe.index, dupe to mutableListOf())
            errorMap[dupe.index]?.second?.add("Duplicate Id ($indexes)")
        }
    }
}

private fun List<Mod>.addDupeFilenames(
    errorMap: MutableMap<Int, Pair<Mod, MutableList<String>>>
) {
    groupBy { it.filePath }.filter { it.value.size > 1 }.map { it.value }.forEach { dupes ->
        val indexes = dupes.map { it.index }
        dupes.forEach { dupe ->
            errorMap.putIfAbsent(dupe.index, dupe to mutableListOf())
            errorMap[dupe.index]?.second?.add("Duplicate Filepath ($indexes)")
        }
    }
}


private fun List<Mod>.detectStagingIssues(
    errorMap: MutableMap<Int, Pair<Mod, MutableList<String>>>
) {
    forEach { mod ->
        val stageFolder = File(mod.filePath)
        if (stageFolder.exists()) {
            when (detectStagingChanges(stageFolder)) {
                StageChange.UNKNOWN -> {
                    if (!mod.hasTag(Tag.SKIP_VALIDATE)) {
                        errorMap.putIfAbsent(mod.index, mod to mutableListOf())
                        errorMap[mod.index]?.second?.add("Unable to guess folder path. You should open the staging folder and make sure it was installed correctly.")
                    }
                }

                StageChange.FOMOD -> {
                    errorMap.putIfAbsent(mod.index, mod to mutableListOf())
                    errorMap[mod.index]?.second?.add("FOMOD detected. You should open the staging folder and pick options yourself.")
                }

                StageChange.NO_FILES -> {
                    errorMap.putIfAbsent(mod.index, mod to mutableListOf())
                    errorMap[mod.index]?.second?.add("No files found in stage folder.")
                }

                else -> {}
            }
        }
    }
}

private fun List<Mod>.detectDupePlugins() {
    flatMap { mod ->
        mod.getModFiles().filter { it.extension.lowercase() in listOf("esp", "esm", "esl") }.map { it to mod.index }
    }
        .groupBy { it.first.name }
        .filter { it.value.size > 1 }
        .map { (fileName, indexList) ->
            fileName to indexList.groupBy { it.second }.keys
        }
        .forEach { (name, indexList) ->
            val modNames = indexList.joinToString(", ") { "$it ${toolData.byIndex(it)?.name}" }
            println("$name is duplicated in $modNames")
        }
}

private fun List<Mod>.detectIncorrectCasing(
    errorMap: MutableMap<Int, Pair<Mod, MutableList<String>>>
) {
    val goodPaths = listOf("Data") + gameMode.generatedPaths.values.map { it.suffix }
    forEach { mod ->
        val modsPaths = mod.getModFiles()
            .asSequence()
            .map { it.parent }
            .mapNotNull { file ->
                val start = goodPaths.maxOf { file.indexOf(it) + it.length }
                val end = file.lastIndexOf("/")
                if (start < end) file.substring(start, end) else null
            }.toSet()
            .filter { it.isNotEmpty() &&  it != it.lowercase() }
            .toList()
        if (modsPaths.isNotEmpty()) {
            errorMap.putIfAbsent(mod.index, mod to mutableListOf())
            errorMap[mod.index]?.second?.add("Filepaths should be lowercase between data and filename:")
            modsPaths.forEach {
                errorMap[mod.index]?.second?.add("\t${it}")
            }
        }
    }
}

private fun Map<Mod, List<File>>.detectTopLevelFiles(
    errorMap: MutableMap<Int, Pair<Mod, MutableList<String>>>
) {
    val excludeList = listOf("sfse_loader.exe")
    filter { (mod, files) ->
        !mod.hasTag(Tag.SKIP_VALIDATE) &&
                files.none { excludeList.contains(it.name) } &&
                files.any { !it.path.contains("Data/") }
    }.forEach { (mod, _) ->
        errorMap.putIfAbsent(mod.index, mod to mutableListOf())
        errorMap[mod.index]?.second?.add("Has files outside the Data folder")
    }
}

private fun checkPlugins(
    errorMap: MutableMap<Int, Pair<Mod, MutableList<String>>>,
    helpMessages: MutableSet<String>
) {
    toolData.mods.filter { !it.hasTag(Tag.EXTERNAL) }.forEach { mod ->
        val newPlugins = mod.discoverPlugins().sorted().toSet()
        val existing = mod.plugins.sorted().toSet()

        if (existing != newPlugins) {
            errorMap.putIfAbsent(mod.index, mod to mutableListOf())
            val added = newPlugins - existing
            val removed = existing - newPlugins
            errorMap[mod.index]?.second?.add("Has an out of date plugin list: Added [${green(added.joinToString(", "))}], Removed [${red(removed.joinToString(", "))}]")
            helpMessages.add("To fix plugin issues, run 'esp refresh'")
        }
    }
}

private fun checkCreations(errors: MutableList<String>, helpMessages: MutableSet<String>) {
    parseCreationCatalog().values.filter { creation -> (creation.creationId?.let { toolData.byCreationId(it) } == null) }.forEach { creation ->
        errors.add("Creation '${creation.title}' is not managed")
        helpMessages.add("To manage creations try 'help creation'")
    }
}

private fun checkExternalMods(errors: MutableList<String>, helpMessages: MutableSet<String>) {
    getExternalMods().filter { it.value == null }.keys.forEach {
        errors.add("External Mod '$it' is not managed")
        helpMessages.add("To manage external plugins try 'help external'")
    }
}

private fun printErrors(errorMap: Map<Int, Pair<Mod, MutableList<String>>>) {
    errorMap.entries.forEach { (i, errorList) ->
        val (mod, errors) = errorList
        println("$i ${yellow(mod.name)} has issues:")
        errors.forEach { error ->
            println("\t$error")
        }
    }
}
