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
        getConfigPath("starfield-mod-manager-config.json", "starfield-config.json"),
        "./starfield-data.json",
        "/Data",
        "starfield-mods",
        "starfield",
        starfieldPaths(),
    ),
    OBLIVION_REMASTERED(
        "Oblivion Remastered",
        "or",
        "2623190",
        getConfigPath("oblivion-remastered-mod-manager-config.json", "oblivion-remastered-config.json"),
        "./oblivion-remastered-data.json",
        "/Data",
        "oblivion-remastered-mods",
        "oblivionremastered",
        oblivionRemasteredPaths(),
    );

    fun path(type: PathType) = generatedPaths[type]?.path()
}

fun mainConfigPath() = getConfigPath("mod-manager-config.json", "config.json")

private fun getConfigPath(nameInHome: String, nameLocal: String): String {
    return System.getenv("XDG_CONFIG_HOME")?.replace($$"$HOME", HOME)?.let { "$it/$nameInHome" } ?: "./$nameLocal"
}
