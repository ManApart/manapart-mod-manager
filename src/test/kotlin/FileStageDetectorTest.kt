import io.mockk.every
import io.mockk.mockk
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

class FileStageDetectorTest {

    @Test
    fun happyPath() {
        val root = mockk<File>()
        every { root.listFiles() } returns emptyArray<File>()

        val data = mockk<File>()
        every { data.name } returns "Data"
        every { data.isDirectory } returns true
        every { data.listFiles() } returns emptyArray<File>()
        val actual = detectStagingChanges(root, arrayOf(data))
        assertEquals(StageChange.NONE, actual)
    }

    @Test
    fun noFiles() {
        val root = mockk<File>()
        val actual = detectStagingChanges(root, arrayOf())
        assertEquals(StageChange.NO_FILES, actual)
    }
}
