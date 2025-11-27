fun String?.truncate(length: Int = 6): String {
    return this?.substring(0, kotlin.math.min(this.length, length)) ?: ""
}

data class Table(val columns: List<Column>, val data: List<Map<String, Any>>) {
    fun print(highlightHeaders: Boolean = true) {
        val colFormat = columns.joinToString("") { "%-${it.size}s" }
        val rowFormat = columns.joinToString("") {
            val type = if (it.isNumber) "d" else "s"
            "%-${it.size}$type"
        }

        val headerValues = columns.map { it.header }.toTypedArray()
        val headers = if (highlightHeaders) cyan("$colFormat\n") else "$colFormat\n"
        System.out.printf(headers, *headerValues)
        data.forEachIndexed { i, row ->
            if (i % 10 == 0 && i != 0) println()
            val dataValues = columns.map { row[it.header] ?: "" }.toTypedArray()
            System.out.printf("$rowFormat\n", *dataValues)
        }
    }
}

data class Column(val header: String, val size: Int, val isNumber: Boolean = false)

@JvmName("toTableIntMap")
fun Map<Int, String>.toTable(keyHeader: String, keyLength: Int, valueHeader: String, valueLength: Int) = entries.associate { (k, v) -> k.toString() to v }.toTable(keyHeader, keyLength, valueHeader, valueLength)

fun Map<String, String>.toTable(keyHeader: String, keyLength: Int, valueHeader: String, valueLength: Int) = entries.toList().toTable(keyHeader,keyLength,valueHeader,valueLength)

fun List<Map.Entry<String, String>>.toTable(keyHeader: String, keyLength: Int, valueHeader: String, valueLength: Int): Table {
    val columns = listOf(Column(keyHeader, keyLength), Column(valueHeader, valueLength))
    val data = map { (k, v) ->
        mapOf(keyHeader to k, valueHeader to v)
    }
    return Table(columns, data)
}
