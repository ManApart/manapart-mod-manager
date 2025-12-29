package commands.deploy

import Mod
import PathType
import commands.deploy.addModFiles
import cyan
import gameConfig
import gameMode
import toolData
import verbose
import yellow
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import kotlin.io.path.Path

val deployDescription = """
    Applies all enabled mods to the game folder by creating the appropriate symlinks
    Overrides shows any mods that conflict with other mods and gives their load order in parenthesis and then the index of the mod
    Dryrun shows a detailed view of how files will be deployed, without deploying them
""".trimIndent()

val deployUsage = """
    deploy
    deploy overrides
    deploy dryrun
""".trimIndent()

fun deploy(command: String, args: List<String>) {
    val files = getAllModFiles(true)
    when {
        args.firstOrNull() == "dryrun" -> deployDryRun(files)
        args.firstOrNull() == "overrides" -> showOverrides()
        files.isEmpty() -> println(yellow("No mod files found"))
        else ->  {
            deployPlugins(files)
            getAllModFilesByTarget(true).entries.forEach { (target, f) -> doDeploy(f, target) }
        }
    }
}

private fun getAllModFiles(logMissing: Boolean = false): Map<String, File> {
    val mappings = mutableMapOf<String, File>()
    toolData.mods.filter { it.enabled }.sortedBy { it.loadOrder }.forEach { mappings.addModFiles(it, logMissing) }
    return mappings
}

private fun getAllModFilesByTarget(logMissing: Boolean = false): Map<PathType, Map<String, File>> {
    val mappings = mutableMapOf<PathType, MutableMap<String, File>>()
    toolData.mods.filter { it.enabled }.sortedBy { it.loadOrder }.forEach {
        mappings.putIfAbsent(it.deployTarget, mutableMapOf())
        mappings[it.deployTarget]?.addModFiles(it, logMissing)
    }
    return mappings
}

private fun doDeploy(files: Map<String, File>, target: PathType) {
    getDisabledModPaths(target).forEach { deleteLink(target, it, files) }
    files.entries.forEach { (gamePath, modFile) -> makeLink(modFile, target, gamePath) }
    println(cyan("Deployed ${files.size} files to $target folder"))
}

private fun getDisabledModPaths(target: PathType): List<String> {
    return toolData.mods.filter { !it.enabled && it.deployTarget == target }.flatMap { mod ->
        mod.getModPaths()
    }
}

private fun MutableMap<String, File>.addModFiles(mod: Mod, logMissing: Boolean = false) {
    val modRoot = File(mod.filePath).absolutePath + "/"
    mod.getModFiles().also { if (logMissing && it.isEmpty()) println(yellow("No files found for ${mod.name}")) }.forEach { file ->
        this[file.absolutePath.replace(modRoot, "")] = file
    }
}

fun makeLink(modFile: File, target: PathType, gamePath: String) {
    val gameFile = getGameFile(target, gamePath)
    if (gameFile == null) {
        println("Unable to find game file for $target and path $gamePath. Please check your deploytarget for this mod and make sure the path exists. See detail and mod commands.")
        return
    }
    gameFile.parentFile.mkdirs()
    if (Files.isSymbolicLink(gameFile.toPath())) {
        val existingLink = Files.readSymbolicLink(gameFile.toPath())
        if (existingLink != modFile.canonicalFile.toPath()) {
            println("Update: ${modFile.path}")
            gameFile.delete()
            Files.createSymbolicLink(gameFile.toPath(), modFile.canonicalFile.toPath())
        } else verbose("Skip: ${modFile.path}")
    } else if (gameFile.exists()) {
        verbose("Backup: ${gameFile.path}")
        verbose("Add: ${modFile.path}")
        Files.move(
            gameFile.toPath(),
            Path("${gameFile.parentFile.absolutePath}/${gameFile.nameWithoutExtension}_overridden.${gameFile.extension}"),
            StandardCopyOption.REPLACE_EXISTING
        )
        Files.createSymbolicLink(gameFile.toPath(), modFile.canonicalFile.toPath())
    } else {
        verbose("Add: ${modFile.path}")
        Files.createSymbolicLink(gameFile.toPath(), modFile.canonicalFile.toPath())
    }
}

fun deleteLink(target: PathType, gamePath: String, modFiles: Map<String, File>) {
    val gameFile = getGameFile(target, gamePath)
    if (gameFile == null) {
        println("Unable to find game file for $target and path $gamePath. Please check your deploytarget for this mod and make sure the path exists. See detail and mod commands.")
        return
    }
    if (!modFiles.contains(gamePath) && Files.isSymbolicLink(gameFile.toPath())) {
        verbose("Delete: $gamePath")
        gameFile.delete()
        val backedUpFile =
            File("${gameFile.parentFile.absolutePath}/${gameFile.nameWithoutExtension}_overridden.${gameFile.extension}")
        if (backedUpFile.exists()) {
            verbose("Restore: ${gameFile.path}")
            Files.move(backedUpFile.toPath(), gameFile.toPath())
        }
    }
}

private fun getGameFile(target: PathType, gamePath: String): File? {
    val base = gameMode.path(target) ?: return null
    val parent = if (target == PathType.DATA) {
        //Since we require data files under the data folder, strip off the double data folder
        base.split("/").dropLast(1).joinToString("/")
    } else base
    return File("$parent/$gamePath")
}
