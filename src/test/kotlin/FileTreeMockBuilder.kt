import java.io.File

class FileTreeMockBuilder(val name: String) {
    private val childFolders = mutableListOf<FileTreeMockBuilder>()
    private val childFiles = mutableListOf<File>()

    fun folder(builder: FileTreeMockBuilder) {
        childFolders.add(builder)
    }

    fun folder(name: String) {
        childFolders.add(FileTreeMockBuilder(name))
    }

    fun file(file: File) {
        childFiles.add(file)
    }

    fun file(file: String) {
        childFiles.add(mockFile(file))
    }

    fun build(): File {
        return mockFolder(name, childFolders.map { it.build() } + childFiles)
    }
}

fun folder(name: String, initializer: FileTreeMockBuilder.() -> Unit): File {
    return FileTreeMockBuilder(name).apply(initializer).build()
}
