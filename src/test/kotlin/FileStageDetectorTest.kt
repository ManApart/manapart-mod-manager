import kotlin.test.Test
import kotlin.test.assertEquals

class FileStageDetectorTest {

    @Test
    fun happyPath() {
        val root = folder("test-mod") {
            folder("Data")
        }

        val actual = detectStagingChanges(root)
        assertEquals(StageChange.NONE, actual)
    }

    @Test
    fun noFiles() {
        val root = mockFolder("test-mod")
        val actual = detectStagingChanges(root, arrayOf())
        assertEquals(StageChange.NO_FILES, actual)
    }

    @Test
    fun removeWinGDK() {
        val root = folder("test-mod") {
            folder("Win64")
            folder("WinGDK")
        }
        val actual = detectStagingChanges(root)
        assertEquals(StageChange.REMOVE_WINGDK, actual)
    }

    @Test
    fun detectFOMOD() {
        val root = folder("test-mod") {
            folder("FOMOD")
        }
        val actual = detectStagingChanges(root)
        assertEquals(StageChange.FOMOD, actual)
    }

    @Test
    fun capitalize() {
        val root = folder("test-mod") {
            folder("data")
        }
        val actual = detectStagingChanges(root)
        assertEquals(StageChange.CAPITALIZE, actual)
    }

    @Test
    fun nestInData() {
        val actuals = listOf("textures", "music", "sound", "meshes", "video").map { dataTopLevel ->
            val root = folder("test-mod") {
                folder(dataTopLevel)
            }
            detectStagingChanges(root)
        }.toSet()
        assertEquals(1, actuals.size)
        assertEquals(StageChange.NEST_IN_DATA, actuals.first())
    }

    @Test
    fun nestInDataExtensions() {
        val actuals = listOf("esp", "esm", "ba2").map { dataTopLevel ->
            val root = folder("test-mod") {
                file("thing.$dataTopLevel")
            }
            detectStagingChanges(root)
        }.toSet()
        assertEquals(1, actuals.size)
        assertEquals(StageChange.NEST_IN_DATA, actuals.first())
    }
}
