import java.io.File

enum class StageChange { NONE, NEST_IN_DATA, USE_WIN64, USE_PAK, USE_UE4SS, USE_OBSE, USE_SFSE, ADD_TOP_FOLDER, REMOVE_TOP_FOLDER, UNNEST, FOMOD, CAPITALIZE, NO_FILES, REMOVE_WINGDK, UNKNOWN }

fun detectStagingChanges(stageFolder: File, stagedFiles: Array<File> = stageFolder.listFiles() ?: arrayOf()): StageChange {
    val stagedNames = stagedFiles.map { it.nameWithoutExtension.lowercase() }
    val stagedExtensions = stagedFiles.map { it.extension }
    val dataTopLevelNames = listOf("textures", "music", "sound", "meshes", "video")
    val dataTopLevelExtensions = listOf("esp", "esm", "ba2")
    val nestableExtensions = listOf("pak")
    val validTopLevelFiles = listOf("engine")
    val validTopLevelFolders = listOf("data")
    val firstFolder = stagedFiles.firstOrNull { it.isDirectory }
    val hasNested = firstFolder != null
    val nestedFiles = if (hasNested) firstFolder.listFiles() ?: arrayOf() else arrayOf()
    val allFiles = lazy { stageFolder.getFiles() }
    return when {
        stagedFiles.isEmpty() -> StageChange.NO_FILES
        stagedNames.size == 2 && stagedNames.contains("win64") && stagedNames.contains("wingdk") -> StageChange.REMOVE_WINGDK
        stagedNames.contains("fomod") -> StageChange.FOMOD
        stagedFiles.any { validTopLevelFolders.contains(it.nameWithoutExtension) } -> StageChange.CAPITALIZE
        stagedNames.any { dataTopLevelNames.contains(it) } -> StageChange.NEST_IN_DATA
        stagedNames.any { it.startsWith("obse64") } -> StageChange.USE_WIN64
        stagedExtensions.any { dataTopLevelExtensions.contains(it) } -> StageChange.NEST_IN_DATA
        stagedExtensions.any { "pak" == it } -> StageChange.USE_PAK
        hasNested && nestedFiles.map { it.nameWithoutExtension.lowercase() }.contains("data") -> StageChange.UNNEST
        stagedFiles.size == 1 && stagedFiles.first().extension == "dll" -> StageChange.USE_WIN64
        stagedFiles.any { it.name.lowercase() == "enabled.txt" } -> StageChange.ADD_TOP_FOLDER
        hasNested && stagedFiles.size == 1 && nestedFiles.map { it.extension }.any { dataTopLevelExtensions.contains(it) || nestableExtensions.contains(it) } -> StageChange.REMOVE_TOP_FOLDER
        hasNested && stagedFiles.size == 1 && nestedFiles.map { it.nameWithoutExtension.lowercase() }
            .any { validTopLevelFolders.contains(it) || validTopLevelFiles.contains(it) } -> StageChange.REMOVE_TOP_FOLDER

        hasNested && stagedFiles.size == 1 && nestedFiles.any { it.name.lowercase() == "enabled.txt" } -> StageChange.USE_UE4SS
        allFiles.value.any { it.absolutePath.lowercase().contains("obse/plugins") } -> StageChange.USE_OBSE
        allFiles.value.any { it.absolutePath.lowercase().contains("sfse/plugins") } -> StageChange.USE_SFSE
        hasNested && stagedFiles.size == 1 && firstFolder.name.lowercase() != "data" -> StageChange.REMOVE_TOP_FOLDER
        stagedNames.contains("dwmapi") && stagedNames.contains("ue4ss") -> StageChange.USE_WIN64
        hasNested && firstFolder.name == "ue4ss" && nestedFiles.any { it.name == "Mods" } -> StageChange.CAPITALIZE
        stagedNames.any { validTopLevelFiles.contains(it) } -> StageChange.NONE
        stagedNames.any { validTopLevelFolders.contains(it) } -> StageChange.NONE
        else -> StageChange.UNKNOWN
    }
}
