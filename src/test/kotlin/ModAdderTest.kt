import nexus.ModFileInfo
import nexus.ModFileInfoFile
import kotlin.test.Test
import kotlin.test.assertEquals

class ModAdderTest {

    @Test
    fun basicPrimaryFile(){
        val input = ModFileInfo(listOf(
            ModFileInfoFile(1, "1", "1.zip","0.1", false),
            ModFileInfoFile(2, "2", "1.zip", "0.0.1", true),
        ))
        val actual = input.getPrimaryFile()
        assertEquals(2, actual)
    }

    @Test
    fun singleFile(){
        val input = ModFileInfo(listOf(
            ModFileInfoFile(1, "1", "1.zip", "0.1", false),
        ))
        val actual = input.getPrimaryFile()
        assertEquals(1, actual)
    }

    @Test
    fun versionFallback(){
        val input = ModFileInfo(listOf(
            ModFileInfoFile(1, "1", "1.zip", "0.0.1", false),
            ModFileInfoFile(2, "2", "2.zip", "0.0.2", false),
        ))
        val actual = input.getPrimaryFile()
        assertEquals(2, actual)
    }

    @Test
    fun majorBeatsMinor(){
        val input = ModFileInfo(listOf(
            ModFileInfoFile(1, "1", "1.zip", "1.0.1", false),
            ModFileInfoFile(2, "2", "2.zip", "0.0.2", false),
        ))
        val actual = input.getPrimaryFile()
        assertEquals(1, actual)
    }


}
