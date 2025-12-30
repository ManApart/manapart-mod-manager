import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import kotlin.io.path.Path
import kotlin.io.path.exists

fun stageMod(sourceFile: File, stageFolder: File, mod: Mod): Boolean {
    stageFolder.mkdirs()
    stageFolder.backupIni()
    stageFolder.listFiles()?.forEach { it.deleteRecursively() }
    return when {
        sourceFile.isDirectory -> {
            sourceFile.copyRecursively(stageFolder, overwrite = true)
            true
        }

        sourceFile.extension == "7z" || sourceFile.extension == "zip" -> {
            stageFolder.runCommand(listOf("7z", "x", "-y", sourceFile.absolutePath), !toolConfig.verbose)
            true
        }

        sourceFile.extension == "rar" -> {
            stageFolder.runCommand(listOf("bsdtar", "-xf", sourceFile.absolutePath), !toolConfig.verbose)
            true
        }

        else -> {
            println("Unknown Filetype: ${sourceFile.extension}")
            false
        }
    }.also { success ->
        if (success) fixFolderPath(mod, stageFolder)
    }
}

private fun fixFolderPath(mod: Mod, stageFolder: File, count: Int = 0) {
    val stagedFiles = stageFolder.listFiles() ?: arrayOf()
    val action = detectStagingChanges(stageFolder, stagedFiles)
    if (count > 20) {
        println(yellow("Unable to fix folder path. You should open the staging folder and make sure it was installed correctly."))
        return
    }
    when (action) {
        StageChange.NONE -> {}
        StageChange.NO_FILES -> println(yellow("No staged files found for ${mod.name}"))
        StageChange.CAPITALIZE -> capitalize(stageFolder)
        StageChange.NEST_IN_DATA -> nestInPrefix(mod.name, gameMode.deployedModPath, stageFolder, stagedFiles)
        StageChange.USE_WIN64 -> {
            mod.setDeployTarget(PathType.WIN64)
            capitalize(stageFolder)
        }

        StageChange.USE_PAK -> mod.setDeployTarget(PathType.PAKS)
        StageChange.USE_UE4SS -> mod.setDeployTarget(PathType.UE4SS_MODS)
        StageChange.USE_SCRIPT_EXTENDER -> {
            mod.setDeployTarget(PathType.SCRIPT_EXTENDER_PLUGINS)
            val filter: (File) -> Boolean = if (gameMode == GameMode.OBLIVION_REMASTERED) { it -> !it.path.lowercase().contains("wingdk") } else { _ -> true }
            fullyUnnestLeafFiles(stageFolder, filter)
        }

        StageChange.UNNEST -> unNestFiles(stageFolder, stagedFiles)
        StageChange.ADD_TOP_FOLDER -> {
            nestInPrefix(mod.name, "/" + stageFolder.name, stageFolder, stagedFiles)
            fixFolderPath(mod, stageFolder, count + 1)
        }

        StageChange.REMOVE_TOP_FOLDER -> {
            unNestFiles(stageFolder, stagedFiles)
            fixFolderPath(mod, stageFolder, count + 1)
        }

        StageChange.REMOVE_WINGDK -> {
            stagedFiles.first { it.nameWithoutExtension.lowercase() == "wingdk" }.deleteRecursively()
            fixFolderPath(mod, stageFolder, count + 1)
        }

        StageChange.FOMOD -> println(yellow("FOMOD detected for ${mod.name}.") + " You should open the staging folder and pick options yourself.")
        else -> println(yellow("Unable to guess folder path for ${mod.name}.") + " You should open the staging folder and make sure it was installed correctly.")
    }
    properlyCasePaths(stageFolder)
}

private fun Mod.setDeployTarget(target: PathType) {
    deployTarget = target
    save()
}

fun unNestFiles(stageFolder: File, stagedFiles: Array<File>) {
    val topFolder = stagedFiles.first()
    try {
        topFolder.listFiles()?.forEach { nested ->
            unNest(stageFolder.path, nested, topFolder.path)
        }
        topFolder.deleteRecursively()
    } catch (e: Exception) {
        println("Failed to unnest files. Please fix manually")
        verbose(e.message ?: "")
        verbose(e.stackTraceToString())
    }
}

private fun unNest(stageFolderPath: String, nested: File, topPath: String) {
    val newPath = stageFolderPath + nested.path.replace(topPath, "").replace("/data", "/Data")
    Files.move(nested.toPath(), Path(newPath), StandardCopyOption.REPLACE_EXISTING)
    if (nested.isDirectory) {
        nested.listFiles()?.forEach { moreNested ->
            unNest(stageFolderPath, moreNested, topPath)
        }
    }
}

fun fullyUnnestLeafFiles(stageFolder: File, filterFunction: (File) -> Boolean = { true }) {
    stageFolder.getFiles(filterFunction).filter { it.isFile }.forEach {
        Files.move(it.toPath(), Path(stageFolder.path + "/" + it.name), StandardCopyOption.REPLACE_EXISTING)
    }
    stageFolder.listFiles()!!.filter { it.isDirectory }.forEach { it.deleteRecursively() }
}

fun nestInPrefix(modName: String, prefix: String, stageFolder: File, stagedFiles: Array<File>) {
    println("Nesting files in $prefix for $modName")
    try {
        val dataFolder = File(stageFolder.path + prefix).also { it.mkdirs() }
        stagedFiles.forEach { file ->
            nest(stageFolder.path, file, dataFolder.path)
        }
    } catch (e: Exception) {
        println("Failed to nest files. Please fix manually")
        verbose(e.message ?: "")
        verbose(e.stackTraceToString())
    }
}

private fun nest(stageFolderPath: String, file: File, dataPath: String) {
    val newPath = Path(file.path.replace(stageFolderPath, dataPath))
    Files.move(file.toPath(), newPath, StandardCopyOption.REPLACE_EXISTING)
    if (file.isDirectory) {
        file.listFiles()?.forEach { moreNested ->
            nest(stageFolderPath, moreNested, dataPath)
        }
    }
}

private fun capitalize(stageFolder: File) {
    Path(stageFolder.path + "/data").takeIf { it.exists() }?.let { Files.move(it, Path(stageFolder.path + "/Data")) }
    Path(stageFolder.path + "/ue4ss/Mods").takeIf { it.exists() }?.let { Files.move(it, Path(stageFolder.path + "/ue4ss/mods")) }
}

private fun properlyCasePaths(stageFolder: File) {
    File(stageFolder.absolutePath).listFiles()?.filter { it.isDirectory }?.forEach { case(it) }
}

private fun case(folder: File) {
    val ignored = listOf("Content", "Dev", "ObvData", "Data", "Binaries", "Win64", "Mods", "Paks")
    val newPath = folder.parent + "/" + folder.name.lowercase()
    val next = if (!ignored.contains(folder.name)) {
        Files.move(folder.toPath(), Path(newPath), StandardCopyOption.REPLACE_EXISTING)
        File(newPath)
    } else folder
    if (!next.canExecute()) next.setExecutable(true, false)
    next.listFiles()?.filter { it.isDirectory }?.forEach { case(it) }
}

private fun File.backupIni() {
    getAllFiles(listOf(".git")).filter { it.extension == "ini" }.forEach { ini ->
        val downloadFolder = File("$HOME/Downloads/${gameMode.modFolder}/ini").also { it.mkdirs() }
        val number = downloadFolder.listFiles()
            .filter { it.nameWithoutExtension.startsWith(ini.nameWithoutExtension) }
            .maxOfOrNull { it.nameWithoutExtension.split("-").last().toIntOrNull() ?: 0 }?.let { it + 1 } ?: 0
        val target = Path(downloadFolder.path + "/${ini.nameWithoutExtension}-$number.ini")
        Files.copy(ini.toPath(), target)
        println(cyan("Backed up ini to ") + target)
    }
}

private fun File.getAllFiles(ignore: List<String>): List<File> {
    return listFiles().filter { !ignore.contains(it.name) }
        .flatMap { if (it.isDirectory) it.getAllFiles(ignore) else listOf(it) }
}
