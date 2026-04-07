enum class GameMode(
    val displayName: String,
    val abbreviation: String,
    val steamId: String,
    val configPath: String,
    val dataJsonPath: String,
    val deployedModPath: String,
    val modFolder: String,
    val urlName: String,
    val generatedPaths: Map<PathType, GeneratedPath>
) {
    STARFIELD(
        "Starfield",
        "sf",
        "1716740",
        getConfigPath("starfield-config.json"),
        getConfigPath("./starfield-data.json"),
        "/Data",
        "starfield-mods",
        "starfield",
        starfieldPaths(),
    ),
    OBLIVION_REMASTERED(
        "Oblivion Remastered",
        "or",
        "2623190",
        getConfigPath("oblivion-remastered-config.json"),
        getConfigPath("./oblivion-remastered-data.json"),
        "/Data",
        "oblivion-remastered-mods",
        "oblivionremastered",
        oblivionRemasteredPaths(),
    );

    fun path(type: PathType) = generatedPaths[type]?.path()
}

fun mainConfigPath() = getConfigPath("config.json")

private fun getConfigPath(name: String): String {
    return System.getenv("XDG_CONFIG_HOME")?.replace($$"$HOME", HOME)?.let { "$it/mmm/$name" } ?: "$HOME/mmm/$name"
}
