package commands

import Column
import Mod
import Profile
import Table
import confirmation
import save
import toolData

val profileDescription = """
    Allows you to create new profiles, and then view, save, or load them by index.
    Profiles are like local mod collections and save all of the enabled / disabled mods.
    This should let you quickly bulk enable / disable mods for different scenarios.
    Compare profile allows you to see what the difference is between your currently enabled mods and the profile's mods
""".trimIndent()

val profileUsage = """
    profile list
    profile save <name> 
    profile save <index>
    profile view <index>
    profile load <index>
    profile compare <index>
""".trimIndent()

fun profile(args: List<String> = listOf()) {
    val subCommand = args.firstOrNull()
    val index = args.lastOrNull()?.toIntOrNull()
    when {
        subCommand == null || subCommand == "list" -> viewProfiles()
        subCommand == "view" && index != null -> viewProfile(index)
        subCommand == "load" && index != null -> loadProfile(index)
        subCommand == "compare" && index != null -> compareProfile(index)
        subCommand == "save" && index != null -> saveProfile(index)
        subCommand == "save" && args.size > 1 -> saveProfile(args.drop(1).joinToString(" "))
        else -> println(profileDescription)
    }
}

private fun viewProfiles() {
    val columns = listOf(
        Column("Index", 10),
        Column("Mods", 10),
        Column("Name", 22),
    )
    val data = toolData.profiles.mapIndexed { i, profile ->
        mapOf(
            "Index" to i,
            "Name" to profile.name,
            "Mods" to profile.modCount()
        )
    }
    Table(columns, data).print()
}

private fun viewProfile(i: Int) {
    toolData.profileByIndex(i)?.let { profile ->
        println("$i: ${profile.name}")
        val mods =
            profile.ids.mapNotNull { toolData.byId(it) } + profile.filePaths.mapNotNull { toolData.byFilePath(it) }
        println(mods.joinToString("\n") { "\t${it.name}" })
    }
}

private fun loadProfile(i: Int) {
    val profile = toolData.profileByIndex(i)
    if (profile == null) {
        println("Could not find profile for $i")
        return
    }
    toolData.mods.forEach { mod ->
        val enabled = profile.ids.contains(mod.id) || profile.filePaths.contains(mod.filePath)
        enableMod(enabled, mod.index)
    }
    save()
    println("Loaded ${profile.name}")
}
private fun compareProfile(i: Int) {
    val profile = toolData.profileByIndex(i)
    if (profile == null) {
        println("Could not find profile for $i")
        return
    }
    val (enabled, disabled) = toolData.mods.partition { it.enabled }
    val added = disabled.filter { mod ->
        profile.ids.contains(mod.id) || profile.filePaths.contains(mod.filePath)
    }
    val removed = enabled.filter { mod ->
        !profile.ids.contains(mod.id) && !profile.filePaths.contains(mod.filePath)
    }
    println("${profile.name} adds mods:")
    println("\t"+added.joinToString("\n\t") { it.description() })
    println("${profile.name} removes mods:")
    println("\t"+removed.joinToString("\n\t") { it.description() })
}

private fun saveProfile(i: Int) {
    toolData.profileByIndex(i)?.let { profile ->
        println("Save changes to ${profile.name}? (y/n)")
        val newIds = enabledIds()
        val newPaths = enabledPaths()
        printDiff(profile, newIds, newPaths)
        confirmation = { args ->
            if (args.firstOrNull() == "y") {
                profile.ids = newIds
                profile.filePaths = newPaths
                save()
                println("Saved ${profile.name}")
            }
        }
    }
}

private fun saveProfile(name: String) {
    if (toolData.profiles.any { it.name == "name" }) {
        println("Profile already exists with name $name.")
        println("To save to this profile, use the index reference.")
        return
    }
    toolData.profiles.add(Profile(name, enabledIds(), enabledPaths()))
    save()
    println("Saved $name")
}

private fun printDiff(profile: Profile, newIds: List<Int>, newPaths: List<String>) {
    val added = (newIds.filter { !profile.ids.contains(it) }
        .map { it.toString() } + newPaths.filter { !profile.filePaths.contains(it) }).joinToString()
    val removed = (profile.ids.filter { !newIds.contains(it) }
        .map { it.toString() } + profile.filePaths.filter { !newPaths.contains(it) }).joinToString()
    if (added.isNotBlank()) println("Added: $added")
    if (removed.isNotBlank()) println("Removed: $removed")
}

private fun enabledIds() = toolData.mods.filter { it.enabled }.mapNotNull { it.id }

private fun enabledPaths() = toolData.mods.filter { it.enabled && it.id == null }.map { it.filePath }