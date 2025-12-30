import io.mockk.every
import io.mockk.mockk
import java.io.File

fun mockFolder(name: String, children: List<File> = emptyList(), parentPath: String? = null): File {
    val file = mockk<File>()
    every { file.name } returns name
    every { file.absolutePath } returns (parentPath?.let { "$it/" } ?: "") + name
    every { file.isDirectory } returns true
    every { file.listFiles() } returns children.toTypedArray()
    return file
}

fun mockFile(name: String, parentPath: String? = null): File {
    val file = mockk<File>()
    every { file.name } returns name
    every { file.absolutePath } returns (parentPath?.let { "$it/" } ?: "") + name
    every { file.isDirectory } returns false
    every { file.listFiles() } returns emptyArray<File>()
    return file
}
