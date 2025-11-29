package commands

import commands.add.*
import commands.config.*
import commands.deploy.*
import commands.edit.*
import commands.open.*
import commands.update.*
import commands.view.*

enum class Category { ADD, DEPLOY, VIEW, EDIT, OPEN, UPDATE, CONFIG }

enum class CommandType(
    val summary: String,
    val category: Category,
    val description: String,
    val usage: String,
    val apply: (String, List<String>) -> Unit,
    vararg val aliases: String = arrayOf(),
) {
    ABSTAIN("Abstain from endorsing Mod", Category.EDIT, endorseDescription, "abstain <index>", ::abstain),
    ADD("Add a new mod", Category.ADD, addModDescription, addModUsage, ::addMod, "a"),
    CATEGORY("View categories", Category.EDIT, categoryDescription, categoryUsage, ::category, "cat", "categories"),
    CONFIG("Edit Configuration", Category.CONFIG, configDescription, configUsage, ::config, "c"),
    CLI("Open local mod folder in terminal", Category.OPEN, openCliDescription, cliUsage, ::open, "terminal", "term"),
    CREATION("Manage Creation", Category.ADD, creationDescription, creationUsage, ::creation, "creations", "cr"),
    DEPLOY("Deploy enabled mods", Category.DEPLOY, deployDescription, deployUsage, ::deploy, "d"),
    DETAIL("View all details of mod", Category.VIEW, detailDescription, detailUsage, ::detailMod, "dd", "show", "view"),
    DISABLE("Disable Mod", Category.DEPLOY, enableDescription, disableUsage, ::disable, "x"),
    ENABLE("Enable Mod", Category.DEPLOY, enableDescription, enableUsage, ::enable, "e"),
    ENDORSE("Endorse Mod", Category.EDIT, endorseDescription, endorseUsage, ::endorse, "en"),
    ESP("Change Load Order for plugins", Category.DEPLOY, espDescription, espUsage, ::esp, "esps"),
    EXTERNAL("Manage External Plugins", Category.ADD, externalModDescription, externalModUsage, ::externalMod, "ex"),
    FETCH("Fetch Mod Data", Category.ADD, fetchDescription, fetchUsage, ::fetchMod, "f"),
    FILTER("Apply a filter to Mods", Category.VIEW, filterDescription, filterUsage, ::filterMods, "fl"),
    FIND("Find file", Category.VIEW, findDescription, findUsage, ::find, "ff"),
    HELP("Explain commands", Category.CONFIG, helpDescription, helpUsage, ::help, "h"),
    IMPORT("Import from viewer", Category.ADD, importDescription, importUsage, ::importData, "i"),
    LIST("List Mods", Category.VIEW, listDescription, listUsage, ::listMods, "ls"),
    LOCAL("Open local mod folder", Category.OPEN, openLocalDescription, localUsage, ::open, "l"),
    MODE("Switch Games", Category.CONFIG, modeDescription, modeUsage, ::selectGameMode),
    MOD("Update a mod", Category.EDIT, changeDescription, changeUsage, ::changeMod, "m"),
    NEST("Nest mode files", Category.ADD, nestDescription, nestUsage, ::nest, "n"),
    NOTE("Make a log note", Category.CONFIG, noteDescription,noteUsage, ::noteCommand),
    ORDER("Change Load Order", Category.DEPLOY, orderDescription, orderUsage, ::order, "or"),
    OPEN("Open mod on web", Category.OPEN, openDescription,  openUsage, ::open, *openAliases),
    PURGE("Purge all sym links", Category.DEPLOY, purgeDescription, purgeUsage, ::purge),
    PATHS("View game related paths", Category.OPEN, pathsDescription, pathsUsage, ::paths, *pathsAliases),
    PROFILE("Create and use local mod lists", Category.DEPLOY, profileDescription, profileUsage, ::profile, "p", "prof"),
    RENAME("Rename a mod", Category.EDIT, renameHelp, "rename <index> <new name>", ::moveMod, "mv"),
    REFRESH("Refresh mods by id", Category.UPDATE, refreshDescription, refreshUsage, ::refresh, "rr"),
    REMOVE("Delete a mod", Category.ADD, removeHelp, removeDescription, ::remove, "rm", "delete"),
    SEARCH("Search Mods", Category.VIEW, searchDescription, searchModsUsage, ::searchMods, "grep", "awk"),
    SORT("Sort Mods", Category.VIEW, sortDescription, sortUsage, ::sortMods, "s"),
    START("Launch Game", Category.CONFIG, startGameDescription, startGameUsage, ::startGame, "game", "g"),
    CREATIONKIT("Launch Creation Kit", Category.CONFIG, startCKDescription, startSKUsage, ::startCK, "ck", "startck"),
    TAG("Edit tags on a mod", Category.EDIT, tagDescription, tagUsage, ::tag, "t", "tags"),
    UPDATE("Check for newer versions", Category.UPDATE, updateDescription, updateUsage, ::update, "up"),
    UPGRADE("Upgrade to newer versions", Category.UPDATE, upgradeDescription, upgradeUsage, ::upgrade, "ug"),
    VALIDATE("List issues with mods", Category.DEPLOY, validateDescription, validateUsage, ::validateMods, "v"),
    VERSION("See current and new version for mod", Category.UPDATE, versionDescription, versionUsage, ::version, "vv"),
    EXIT(
        "Exit Program", Category.CONFIG,
        "Exit the process",
        "Exit the process",
        { _, _ -> kotlin.system.exitProcess(0) }
    ),
    ;

    constructor(
        summary: String,
        category: Category,
        description: String,
        apply: (String, List<String>) -> Unit,
        vararg aliases: String = arrayOf()
    ) : this(summary, category, description, summary, apply, aliases = aliases)

    val cleanName = name.lowercase().replace("_", "-")
}

fun getCommand(commandString: String) =
    CommandType.entries.firstOrNull {
        commandString == it.cleanName || it.aliases.contains(commandString)
    }
