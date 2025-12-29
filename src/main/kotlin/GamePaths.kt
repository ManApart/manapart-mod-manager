import PathType.*

const val win64 = "/Binaries/Win64"
const val ue4ss = "/Binaries/Win64/ue4ss/Mods"
const val paks = "/Content/Paks/~mods"

enum class GamePath(val description: String, vararg val examples: String) {
    GAME(
        "The path to the folder under steam containing the Data folder. Note its nesting in Oblivion Remastered",
        "/mnt/c/SteamLibrary/steamapps/common/Starfield",
        "/mnt/c/SteamLibrary/steamapps/common/Oblivion Remastered/OblivionRemastered"
    ),
    COMPAT_DATA("The folder is the steam app id under steam compat data", "/mnt/c/SteamLibrary/steamapps/compatdata/2623190");

    fun describe() {
        println(cyan(name) + ": $description")
        println("\t" + examples.joinToString("\n\t") { "config path $name \"$it\"" })
    }
}

enum class PathType(val description: String) {
    GAME("Where the game is installed"),
    JAR("Location the mod manager is running from"),
    DATA("The data path that esps live in"),
    APP_DATA("Appdata in windows. Contains Plugins and ContentCatalog for Starfield. Used for updating mod load order"),
    INI("My Documents in windows. Contains ini files and saves for Starfield. It's optionally used to deploy to your my docs folder instead of the game path"),
    DATA_INI("Ini files above the data folder"),
    UNREAL_INI("Ini files for unreal engine"),
    PLUGINS("The plugins file that activates esps"),
    CATALOG("The catalog file that tracks creations"),
    WIN64("Where the unreal engine binary lives"),
    WINGDK("Folder that UE4SS mods are deployed to for game pass (not used)"),
    UE4SS_Mods("Folder that UE4SS mods are deployed to"),
    PAKS("Where the unreal engine mods live"),
    SAVES("Where your save files are located"),
    STEAM_PICS("Your steam screenshots for the game"),
    SCREENSHOTS("Your game saved screenshots, like photo mode"),
    ;

    companion object {
        fun fromString(path: String) = PathType.entries.firstOrNull { path == it.name.lowercase() }
    }
}

class GeneratedPath(
    val type: PathType,
    val aliases: List<String>,
    private val prefix: GamePath? = null,
    val suffix: String = "",
    private val genPath: (() -> String)? = null,
) {
    fun path() = genPath?.invoke() ?: (prefix?.let { gameConfig[it] } + suffix)
}

private val gamePath = GeneratedPath(GAME, listOf("gamepath", "gg")) { gameConfig[GamePath.GAME] }
private val jarPath = GeneratedPath(JAR, listOf("jarpath", "jar")) { "." }

fun starfieldPaths(): Map<PathType, GeneratedPath> {
    return listOf(
        gamePath,
        jarPath,
        GeneratedPath(DATA, listOf("data")) { gameConfig[GamePath.GAME] + GameMode.STARFIELD.deployedModPath },
        GeneratedPath(APP_DATA, listOf("app", "appdatapath"), GamePath.COMPAT_DATA, "/pfx/drive_c/users/steamuser/AppData/Local/Starfield"),
        GeneratedPath(CATALOG, listOf("catalog"), GamePath.COMPAT_DATA, "/pfx/drive_c/users/steamuser/AppData/Local/Starfield/ContentCatalog.txt"),
        GeneratedPath(INI, listOf("ini", "inipath"), GamePath.COMPAT_DATA, "/pfx/drive_c/users/steamuser/Documents/My Games/Starfield"),
        GeneratedPath(PLUGINS, listOf("plugins", "plugin"), GamePath.COMPAT_DATA, "/pfx/drive_c/users/steamuser/AppData/Local/Starfield/Plugins.txt"),
        GeneratedPath(SAVES, listOf("saves"), GamePath.COMPAT_DATA, "/pfx/drive_c/users/steamuser/Documents/My Games/Starfield/Saves"),
        GeneratedPath(STEAM_PICS, listOf("steampics")) { "$HOME/.steam/debian-installation/userdata/72953529/760/remote/1716740/screenshots" },
        GeneratedPath(SCREENSHOTS, listOf("screenshots", "photos")) { gameConfig[GamePath.GAME] + GameMode.STARFIELD.deployedModPath + "/textures/Photos" },
    ).associateBy { it.type }
}

fun oblivionRemasteredPaths(): Map<PathType, GeneratedPath> {
    return listOf(
        gamePath,
        jarPath,
        GeneratedPath(DATA, listOf("data")) { gameConfig[GamePath.GAME] + GameMode.OBLIVION_REMASTERED.deployedModPath },
        GeneratedPath(DATA_INI, listOf("ini", "inipath"), GamePath.GAME, "/Content/Dev/ObvData/"),
        GeneratedPath(UNREAL_INI, listOf("unrealini", "engineini"), GamePath.COMPAT_DATA, "/pfx/drive_c/users/steamuser/Documents/My Games/Oblivion Remastered/Saved/Config/Windows"),
        GeneratedPath(PLUGINS, listOf("plugins", "plugin")) { gameConfig[GamePath.GAME] + "/${GameMode.OBLIVION_REMASTERED.deployedModPath}/Plugins.txt" },
        GeneratedPath(WIN64, listOf("win64"), GamePath.GAME, win64),
        GeneratedPath(UE4SS_Mods, listOf("ue4ss"), GamePath.GAME, ue4ss),
        GeneratedPath(WINGDK, listOf("wingdk"), GamePath.GAME, "/Binaries/wingdk/ue4ss/Mods"),
        GeneratedPath(PAKS, listOf("paks", "unreal-mods"), GamePath.GAME, paks),
        GeneratedPath(SAVES, listOf("saves"), GamePath.COMPAT_DATA, "/pfx/drive_c/users/steamuser/Documents/My Games/Oblivion Remastered/Saved/SaveGames"),
    ).associateBy { it.type }
}
