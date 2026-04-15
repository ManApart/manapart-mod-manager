package commands.edit
val requireDescription = """
    Mark a mod as requiring another mod
    When a mod is enabled, any required mods are also enabled
    When a mod is disabled, any mods depending on that mod are also disabled
    require lists direct requirements of this mod
    require all lists requirements recursively so you can see the full tree
    require child shows children who require this mod
""".trimIndent()

val requireUsage = """
    require 123 
    require 123 all
    require 123 child
    require 123 add 456
    require 123 remove 456
""".trimIndent()

fun require(command: String, args: List<String>) {

}
