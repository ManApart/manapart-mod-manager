package commands.deploy

import Mod
import PathType
import cyan
import toolData
import java.io.File

fun deployDryRun(files: Map<String, File>) {
    getAnnotatedModFiles().forEach { entry ->
        println(entry.key)
        val winner = entry.value.first()
        println("\tDeploy: (${winner.loadOrder}) ${winner.index} ${winner.name} (${winner.deployTarget})")
        entry.value.drop(1).takeIf { it.isNotEmpty() }?.let { others ->
            println("\tNot used: ${others.joinToString { "(${it.loadOrder}) ${it.idName()} (${it.deployTarget})" }}")
        }
    }
    println()
    toolData.mods.filter { it.enabled }.validate()
    println()
    deployPluginsDryRun(files)
    println(cyan("Deploy dryrun complete"))
}

fun showOverrides() {
    val overrideList = getAnnotatedModFiles().values.filter { it.size > 1 }.groupBy { it.first() }
    if (overrideList.isEmpty()){
        println(cyan("There are no mod conflicts"))
    }
    overrideList.forEach { overrideMap ->
        val winner = overrideMap.key
        val losers = overrideMap.value.flatten().toSet().filter { it != winner }
        println("(${winner.loadOrder}) ${winner.index} ${winner.name} overrides:")
        losers.forEach { loser ->
            println("\t(${loser.loadOrder}) ${loser.index} ${loser.name}")
        }
    }
}

fun showOverrides(matchingMod: Mod) {
    val overrideList = getAnnotatedModFiles().values.filter { it.size > 1 }.groupBy { it.first() }
    if (overrideList.isEmpty()){
        println(cyan("There are no mod conflicts"))
    }
    overrideList.filter { it.key == matchingMod || it.value.flatten().contains(matchingMod) }.forEach { overrideMap ->
        val winner = overrideMap.key
        val losers = overrideMap.value.flatten().toSet().filter { it != winner }
        println("(${winner.loadOrder}) ${winner.index} ${winner.name} overrides:")
        losers.forEach { loser ->
            println("\t(${loser.loadOrder}) ${loser.index} ${loser.name}")
        }
    }
}

private fun getAnnotatedModFiles(): Map<String, List<Mod>> {
    val mappings = mutableMapOf<String, MutableList<Mod>>()
    toolData.mods.filter { it.enabled }.sortedByDescending { it.loadOrder }.forEach { mod ->
        val modRoot = File(mod.filePath).absolutePath + "/"
        mod.getModFiles().forEach { file ->
            val key = file.absolutePath.replace(modRoot, "")
            mappings.putIfAbsent(key, mutableListOf())
            mappings[key]?.add(mod)
        }
    }
    return mappings
}
