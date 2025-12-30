package commands.deploy

import GameMode
import Mod
import PathType
import StageChange
import commands.add.Creation
import commands.add.getExternalMods
import commands.add.parseCreationCatalog
import commands.add.parseCreationPlugins
import commands.edit.Tag
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
    val modsToFiles = associateWith { it.getModFiles() }
    val modsWithFiles = modsToFiles.filter { it.value.isNotEmpty() }
    val creationCatalog = if (gameMode == GameMode.STARFIELD) parseCreationCatalog() else mapOf()
    val creations = if (gameMode == GameMode.STARFIELD) parseCreationPlugins(creationCatalog) else listOf()
    val externalMods = if (gameMode == GameMode.STARFIELD) getExternalMods(creations) else mapOf()

    addDupeIds(errorMap)
    addDupeFilenames(errorMap)
    detectStagingIssues(errorMap, helpMessages)
    detectDupePlugins()
    detectIncorrectCasing(errorMap)
    modsToFiles.checkHasFiles(errorMap)
    modsToFiles.addEmptyEnabled(errorMap)
    modsWithFiles.detectTopLevelNonDataFiles(errorMap, helpMessages)
    modsWithFiles.noSubDirectories(errorMap, helpMessages)
    detectBadUE4Mods(errorMap, helpMessages)
    checkPlugins(errorMap, helpMessages)

    if (gameMode == GameMode.STARFIELD) {
        checkCreations(nonModErrors, helpMessages, creationCatalog)
        checkExternalMods(nonModErrors, helpMessages, externalMods)
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

    println(cyan("Validated $size mods and ${creations.size} creations, ") + yellow("${filteredErrors.keys.size + nonModErrors.size} mods failed validation"))
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
    errorMap: MutableMap<Int, Pair<Mod, MutableList<String>>>,
    helpMessages: MutableSet<String>
) {
    forEach { mod ->
        val stageFolder = File(mod.filePath)
        if (stageFolder.exists()) {
            when (detectStagingChanges(stageFolder)) {
                StageChange.UNKNOWN -> {
                    if (!mod.hasTag(Tag.SKIP_VALIDATE) && mod.deployTarget == PathType.DATA) {
                        errorMap.putIfAbsent(mod.index, mod to mutableListOf())
                        errorMap[mod.index]?.second?.add("Unable to guess folder path.")
                        helpMessages.add("Open the staging folder for unguessed paths and make sure it was installed correctly. Or change the deploy target.")
                    }
                }

                StageChange.FOMOD -> {
                    errorMap.putIfAbsent(mod.index, mod to mutableListOf())
                    errorMap[mod.index]?.second?.add("FOMOD detected.")
                    helpMessages.add("Open the staging folder of any FOMODs and pick options yourself.")
                }

                StageChange.NO_FILES -> {
                    errorMap.putIfAbsent(mod.index, mod to mutableListOf())
                    errorMap[mod.index]?.second?.add("No files found in stage folder.")
                    helpMessages.add("Mod without files should be refreshed or potentially have its deployment target changed.")
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
    val goodPaths = (gameMode.generatedPaths.values.flatMap { listOf(it.suffix, "/"+ it.suffix.split("/").takeLast(2).joinToString("/")) } + gameMode.deployedModPath).filter { it.isNotBlank() && it != "/" }.toSet()
    forEach { mod ->
        val modsPaths = mod.getModFiles()
            .asSequence()
            .map { it.parent }
            .mapNotNull { file ->
                val lower = file.lowercase()
                goodPaths.firstOrNull { lower.contains(it) && !file.contains(it) }
            }.toSet()
            .toList()
        if (modsPaths.isNotEmpty()) {
            errorMap.putIfAbsent(mod.index, mod to mutableListOf())
            errorMap[mod.index]?.second?.add("Filepaths should be lowercase between top folder and filename:")
            modsPaths.forEach {
                errorMap[mod.index]?.second?.add("\t${it}")
            }
        }
    }
}

private fun Map<Mod, List<File>>.checkHasFiles(
    errorMap: MutableMap<Int, Pair<Mod, MutableList<String>>>
) {
    filter { File(it.key.filePath).exists() && it.value.isEmpty() }.forEach { (mod, _) ->
        errorMap.putIfAbsent(mod.index, mod to mutableListOf())
        errorMap[mod.index]?.second?.add("Has no files")
    }
}

private fun Map<Mod, List<File>>.detectTopLevelNonDataFiles(
    errorMap: MutableMap<Int, Pair<Mod, MutableList<String>>>,
    helpMessages: MutableSet<String>
) {
    val excludeList = listOf("Engine.ini")
    val goodPaths = (gameMode.generatedPaths.values.mapNotNull { it.suffix.split("/").getOrNull(1) } + gameMode.deployedModPath.drop(1)).filter { it.isNotBlank() }.toSet()
    filter { (mod, files) ->
        val parent = files.first().path.split("/").take(2).joinToString("/") + "/"
        !mod.hasTag(Tag.SKIP_VALIDATE) &&
                mod.deployTarget == PathType.DATA &&
                files.none { excludeList.contains(it.name) } &&
                files.any { file -> goodPaths.none { file.path.replace(parent, "").startsWith(it) } }
    }.forEach { (mod, _) ->
        errorMap.putIfAbsent(mod.index, mod to mutableListOf())
        errorMap[mod.index]?.second?.add("Has files outside the Data folder")
        helpMessages.add("To fix files outside of data, change the deployment target (see mod command), skip validating this mod, or use local to open it and manually fix file structure")
    }
}

private fun Map<Mod, List<File>>.noSubDirectories(
    errorMap: MutableMap<Int, Pair<Mod, MutableList<String>>>,
    helpMessages: MutableSet<String>
) {
    val relevantTargets = listOf(PathType.SCRIPT_EXTENDER_PLUGINS, PathType.PAKS)
    filter { (mod, files) -> mod.deployTarget in relevantTargets && files.any { it.isDirectory } }.forEach { (mod, _) ->
        errorMap.putIfAbsent(mod.index, mod to mutableListOf())
        errorMap[mod.index]?.second?.add("Has sub folders that shouldn't exist")
        helpMessages.add("OBSE plugins and paks should be at the root level, without subfolders")
    }
}

private fun List<Mod>.detectBadUE4Mods(
    errorMap: MutableMap<Int, Pair<Mod, MutableList<String>>>,
    helpMessages: MutableSet<String>
) {
    filter { it.deployTarget == PathType.UE4SS_MODS }.filter { mod ->
        val files = File(mod.filePath).listFiles()
        files.size != 1 || files.first().listFiles().none { it.name.lowercase() == "enabled.txt" }
    }.forEach { mod ->
        errorMap.putIfAbsent(mod.index, mod to mutableListOf())
        errorMap[mod.index]?.second?.add("Is an incorrectly set up UE4SS mod")
        helpMessages.add("UE4SS mods should have the mod folder at the top level. Inside that folder there should be an enabled.txt file")
    }
}

private fun List<Mod>.checkPlugins(
    errorMap: MutableMap<Int, Pair<Mod, MutableList<String>>>,
    helpMessages: MutableSet<String>
) {
    filter { !it.hasTag(Tag.EXTERNAL) }.forEach { mod ->
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

private fun checkCreations(errors: MutableList<String>, helpMessages: MutableSet<String>, creations: Map<String, Creation>) {
    creations.values.filter { creation -> (creation.creationId?.let { toolData.byCreationId(it) } == null) }.forEach { creation ->
        errors.add("Creation '${creation.title}' is not managed")
        helpMessages.add("To manage creations try 'help creation'")
    }
}

private fun checkExternalMods(errors: MutableList<String>, helpMessages: MutableSet<String>, externalMods: Map<String, Mod?>) {
    externalMods.filter { it.value == null }.keys.forEach {
        errors.add("External Mod '$it' is not managed")
        helpMessages.add("To manage external plugins try 'help external'")
    }
}

private fun Map<Mod, List<File>>.addEmptyEnabled(
    errorMap: MutableMap<Int, Pair<Mod, MutableList<String>>>
) {
    filter { it.key.enabled && it.value.isEmpty() }.forEach { (mod, _) ->
        errorMap.putIfAbsent(mod.index, mod to mutableListOf())
        errorMap[mod.index]?.second?.add("Enabled but not installed")
    }
}

private fun printErrors(errorMap: Map<Int, Pair<Mod, MutableList<String>>>) {
    errorMap.entries.forEach { (i, errorList) ->
        val (mod, errors) = errorList
        println("$i (${mod.id}) ${yellow(mod.name)} has issues:")
        errors.forEach { error ->
            println("\t$error")
        }
    }
}
