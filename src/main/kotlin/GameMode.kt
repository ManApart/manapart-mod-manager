enum class GameMode(
    val displayName: String,
    val abbreviation: String,
    val steamId: String,
    val configPath: String,
    val dataJsonPath: String,
    val deployedModPath: String,
    val modFolder: String,
    val urlName: String,
    val usedGamePath: (String, Boolean) -> String,
    val generatedPaths: Map<PathType, GeneratedPath>
) {
    STARFIELD(
        "Starfield",
        "sf",
        "1716740",
        System.getenv("XDG_CONFIG_HOME")?.replace("~", HOME)?.let { "$it/starfield-mod-manager-config.json" } ?: "./starfield-config.json",
        "./starfield-data.json",
        "/Data",
        "starfield-mods",
        "starfield",
        { modGamePath, useMyDocs -> (if (useMyDocs && modGamePath.startsWith("Data", true)) PathType.INI else PathType.APP_DATA).let { gameMode.path(it)!! } },
        starfieldPaths(),
    ),
    OBLIVION_REMASTERED(
        "Oblivion Remastered",
        "or",
        "2623190",
        System.getenv("XDG_CONFIG_HOME")?.replace("~", HOME)?.let { "$it/oblivion-remastered-mod-manager-config.json" } ?: "./oblivion-remastered-config.json",
        "./oblivion-remastered-data.json",
        "/Content/Dev/ObvData/Data",
        "oblivion-remastered-mods",
        "oblivionremastered",
        { _, _ -> gameMode.path(PathType.GAME)!! },
        oblivionRemasteredPaths(),
    );

    fun path(type: PathType) = generatedPaths[type]?.path()
}

fun mainConfigPath() = System.getenv("XDG_CONFIG_HOME")?.replace("~", HOME)?.let { "$it/mod-manager-config.json" } ?: "./config.json"
