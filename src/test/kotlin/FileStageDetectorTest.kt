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
}
