import io.mockk.every
import io.mockk.mockk
import java.io.File

fun mockFile(name: String): File {
    val file = mockk<File>()
    every { file.name } returns name
    every { file.isDirectory } returns false
    every { file.listFiles() } returns emptyArray<File>()
    return file
}

fun mockFolder(name: String, children: List<File> = emptyList()): File {
    val file = mockk<File>()
    every { file.name } returns name
    every { file.isDirectory } returns true
    every { file.listFiles() } returns children.toTypedArray()
    return file
}
