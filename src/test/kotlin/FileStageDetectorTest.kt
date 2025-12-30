import kotlin.test.Test
import kotlin.test.assertEquals

class FileStageDetectorTest {

    @Test
    fun happyPath() {
        val root = mockFolder("test-mod")
        val data = mockFolder("Data")

        val actual = detectStagingChanges(root, arrayOf(data))
        assertEquals(StageChange.NONE, actual)
    }

    @Test
    fun noFiles() {
        val root = mockFolder("test-mod")
        val actual = detectStagingChanges(root, arrayOf())
        assertEquals(StageChange.NO_FILES, actual)
    }
}
