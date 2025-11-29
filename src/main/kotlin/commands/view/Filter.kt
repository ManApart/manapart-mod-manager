package commands.view

val filterDescription = """
    Filter mods that contain the given text (name or category) or status, including missing ids
    Changes persist across future ls until filter clear or filter all is called
    Filter is additive, meaning it further filters whatever is currently displayed
    To do a one time search, see search
""".trimIndent()

val filterUsage = """
    filter <search text>
    filter tag
    filter tag <tag>
    filter 123
    filter enabled
    filter disabled
    filter staged
    filter unstaged
    filter endorsed
    filter unendorsed
    filter abstained
    filter missing - show missing ids
    filter upgrade
""".trimIndent()

fun filterMods(command: String, args: List<String> = listOf()) {
    searchMods(true, args)
}
