import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

const val THUMBS_UP = "\uD83D\uDC4D"
const val THUMBS_DOWN = "\uD83D\uDC4E"
const val FOLDER = "\uD83D\uDCC1"
val ENABLED = Character.toString(0x1f517)!!
val UPDATE = Character.toString(0x1f4e9)!!
private const val ANSI_RESET = "\u001B[0m";

fun red(text: String) = "\u001B[31m$text$ANSI_RESET"
fun green(text: String) = "\u001B[32m$text$ANSI_RESET"
fun yellow(text: String) = "\u001B[33m$text$ANSI_RESET"
fun blue(text: String) = "\u001B[34m$text$ANSI_RESET"
fun purple(text: String) = "\u001B[35m$text$ANSI_RESET"
fun cyan(text: String) = "\u001B[36m$text$ANSI_RESET"
fun white(text: String) = "\u001B[37m$text$ANSI_RESET"
fun grayBG(text: String) = "\u001B[100m$text$ANSI_RESET"
fun hyperLink(text: String, url: String) = if (toolConfig.useLinks) "\u001B]8;;$url\u001B\\$text\u001B]8;;\u001B\\" else text

fun File.runCommand(command: String, silent: Boolean = false, echo: Boolean = false): String? {
    val parts = command.split("\\s".toRegex())
    return runCommand(parts, silent, echo)
}

fun File.runCommand(parts: List<String>, silent: Boolean = false, echo: Boolean = false): String? {
    if (echo) println(parts.joinToString(" "))
    return try {
        val proc = ProcessBuilder(*parts.toTypedArray())
            .directory(this)
            .apply {
                if (!silent) {
                    redirectOutput(ProcessBuilder.Redirect.INHERIT)
                    redirectError(ProcessBuilder.Redirect.INHERIT)
                }
            }
            .start()

        proc.waitFor(60, TimeUnit.MINUTES)
        proc.inputStream.bufferedReader().readText()
    } catch (e: IOException) {
        e.printStackTrace()
        null
    }
}

fun clearConsole() {
    //clear screen
    print("\u001B[2J")
    //clear history
    print("\u001B[3J")
    //move cursor to top of screen
    print("\u001B[1;1H")
    System.out.flush()
}
