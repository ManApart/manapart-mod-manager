

enum class GameMode(
    val displayName: String,
    val steamId: String,
    val configPath: String,
    val dataJsonPath: String,
    val dataModPath: String,
    val modFolder: String,
    val urlName: String,
    val generatedPaths: Map<PathType, GeneratedPath>
) {
    STARFIELD(
        "Starfield",
        "1716740",
        System.getenv("XDG_CONFIG_HOME")?.replace("~", HOME)?.let { "$it/starfield-mod-manager-config.json" } ?: "./starfield-config.json",
        "./starfield-data.json",
        "/Data",
        "starfield-mods",
        "starfield",
        starfieldPaths(),
    ),
    OBLIVION_REMASTERED(
        "Oblivion Remastered",
        "2623190",
        System.getenv("XDG_CONFIG_HOME")?.replace("~", HOME)?.let { "$it/oblivion-remastered-mod-manager-config.json" } ?: "./oblivion-remastered-config.json",
        "./oblivion-remastered-data.json",
        "/Content/Dev/ObvData/Data",
        "oblivion-remastered-mods",
        "oblivionremastered",
       oblivionRemasteredPaths(),
    );

    fun path(type: PathType) = generatedPaths[type]?.path?.invoke()
}

fun mainConfigPath() = System.getenv("XDG_CONFIG_HOME")?.replace("~", HOME)?.let { "$it/mod-manager-config.json" } ?: "./config.json"
