import java.io.File

class FileTreeMockBuilder(val name: String) {
    private val childFolders = mutableListOf<FileTreeMockBuilder>()
    private val childFiles = mutableListOf<String>()

    fun folder(builder: FileTreeMockBuilder) {
        childFolders.add(builder)
    }

    fun folder(name: String) {
        childFolders.add(FileTreeMockBuilder(name))
    }

    fun file(name: String) {
        childFiles.add(name)
    }

    fun build(parentPath: String? = null): File {
        val absolutePath = (parentPath?.let { "$it/" } ?: "") + name
        val builtChildFolders = childFolders.map { it.build(absolutePath) }
        val builtChildFiles = childFiles.map { mockFile(it, absolutePath) }
        return mockFolder(name, builtChildFolders + builtChildFiles, absolutePath)
    }
}

fun folder(name: String, initializer: FileTreeMockBuilder.() -> Unit): File {
    return FileTreeMockBuilder(name).apply(initializer).build()
}
