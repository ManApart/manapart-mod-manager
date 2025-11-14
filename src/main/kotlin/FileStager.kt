import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import kotlin.io.path.Path

fun stageMod(sourceFile: File, stageFolder: File, modName: String): Boolean {
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
        if (success) {
            fixFolderPath(modName, stageFolder)
        }
    }
}

private fun fixFolderPath(modName: String, stageFolder: File, count: Int = 0) {
    val stagedFiles = stageFolder.listFiles() ?: arrayOf()
    val action = detectStagingChanges(stageFolder)
    if (count > 20) {
        println(yellow("Unable to fix folder path. You should open the staging folder and make sure it was installed correctly."))
        return
    }
    when (action) {
        StageChange.NONE -> {}
        StageChange.NO_FILES -> println(yellow("No staged files found for $modName"))
        StageChange.CAPITALIZE -> capitalizeData(stageFolder)
        StageChange.NEST_IN_DATA -> nestInPrefix(modName, gameMode.deployedModPath, stageFolder, stagedFiles)
        StageChange.NEST_IN_WIN64 -> nestInPrefix(modName, win64, stageFolder, stagedFiles)
        StageChange.NEST_IN_PAK -> nestInPrefix(modName, paks, stageFolder, stagedFiles)
        StageChange.NEST_IN_UE4SS -> nestInPrefix(modName, ue4ss, stageFolder, stagedFiles)
        StageChange.UNNEST -> unNestFiles(modName, stageFolder, stagedFiles)
        StageChange.ADD_TOP_FOLDER -> {
            nestInPrefix(modName, "/" + stageFolder.name, stageFolder, stagedFiles)
            fixFolderPath(modName, stageFolder, count + 1)
        }

        StageChange.REMOVE_TOP_FOLDER -> {
            unNestFiles(modName, stageFolder, stagedFiles)
            fixFolderPath(modName, stageFolder, count + 1)
        }

        StageChange.FOMOD -> println(yellow("FOMOD detected for $modName.") + " You should open the staging folder and pick options yourself.")
        else -> println(yellow("Unable to guess folder path for $modName.") + " You should open the staging folder and make sure it was installed correctly.")
    }
    properlyCasePaths(stageFolder)
}

enum class StageChange { NONE, NEST_IN_DATA, NEST_IN_WIN64, NEST_IN_PAK, NEST_IN_UE4SS, ADD_TOP_FOLDER, REMOVE_TOP_FOLDER, UNNEST, FOMOD, CAPITALIZE, NO_FILES, UNKNOWN }

fun detectStagingChanges(stageFolder: File): StageChange {
    val stagedFiles = stageFolder.listFiles() ?: arrayOf()
    val stagedNames = stagedFiles.map { it.nameWithoutExtension.lowercase() }
    val stagedExtensions = stagedFiles.map { it.extension }
    val dataTopLevelNames = listOf("textures", "music", "sound", "meshes", "video", "sfse_readme", "sfse")
    val dataTopLevelExtensions = listOf("esp", "esm", "ba2")
    val nestableExtensions = listOf("pak")
    val validTopLevelFiles = listOf("engine")
    val validTopLevelFoldersSF = listOf("data")
    val validTopLevelFoldersOR = listOf("content", "binaries")
    val validTopLevelFolders = if (gameMode == GameMode.STARFIELD) validTopLevelFoldersSF else validTopLevelFoldersOR
    val firstFile = stagedFiles.firstOrNull()
    val hasNested = firstFile != null && firstFile.isDirectory
    val nestedFiles = if (hasNested) firstFile?.listFiles() ?: arrayOf() else arrayOf()
    return when {
        stagedFiles.isEmpty() -> StageChange.NO_FILES
        stagedNames.any { validTopLevelFiles.contains(it) } -> StageChange.NONE
        stagedFiles.any { validTopLevelFolders.contains(it.nameWithoutExtension) } -> StageChange.CAPITALIZE
        stagedNames.contains("ue4ss") -> StageChange.NEST_IN_WIN64
        stagedNames.any { dataTopLevelNames.contains(it) } -> StageChange.NEST_IN_DATA
        stagedNames.any { it.startsWith("obse64") } -> StageChange.NEST_IN_WIN64
        stagedNames.any { validTopLevelFolders.contains(it) } -> StageChange.NONE
        stagedExtensions.any { dataTopLevelExtensions.contains(it) } -> StageChange.NEST_IN_DATA
        stagedExtensions.any { "pak" == it } -> StageChange.NEST_IN_PAK
        firstFile?.isDirectory ?: false && firstFile?.nameWithoutExtension?.startsWith("sfse_") ?: false -> StageChange.UNNEST
        hasNested && nestedFiles.map { it.nameWithoutExtension.lowercase() }.contains("data") -> StageChange.UNNEST
        stagedFiles.size == 1 && stagedFiles.first().extension == "dll" -> StageChange.NEST_IN_WIN64
        stagedFiles.any { it.name.lowercase() == "enabled.txt" } -> StageChange.ADD_TOP_FOLDER
        hasNested && stagedFiles.size == 1 && nestedFiles.map { it.extension }.any { dataTopLevelExtensions.contains(it) || nestableExtensions.contains(it) } -> StageChange.REMOVE_TOP_FOLDER
        hasNested && stagedFiles.size == 1 && nestedFiles.map { it.nameWithoutExtension.lowercase() }
            .any { validTopLevelFolders.contains(it) || validTopLevelFiles.contains(it) } -> StageChange.REMOVE_TOP_FOLDER

        hasNested && stagedFiles.size == 1 && nestedFiles.any { it.name.lowercase() == "enabled.txt" } -> StageChange.NEST_IN_UE4SS
        stagedNames.contains("fomod") -> StageChange.FOMOD
        else -> StageChange.UNKNOWN
    }
}

fun unNestFiles(modName: String, stageFolder: File, stagedFiles: Array<File>) {
    println("Unnesting files in data for $modName")
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

private fun capitalizeData(stageFolder: File) {
    Files.move(Path(stageFolder.path + "/data"), Path(stageFolder.path + "/Data"))
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
