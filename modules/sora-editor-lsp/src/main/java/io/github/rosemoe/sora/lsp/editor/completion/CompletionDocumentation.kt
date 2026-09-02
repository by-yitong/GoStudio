package io.github.rosemoe.sora.lsp.editor.completion

/**
 * Extracts a single-line summary from a completion item's documentation,
 * so that the completion list can describe a symbol instead of only showing its signature.
 */
fun completionDocumentationSummary(
    documentation: String?,
    symbolName: String? = null,
    maxLength: Int = 80
): String? {
    if (documentation.isNullOrBlank()) {
        return null
    }

    val lines = documentation.replace("\r\n", "\n").split('\n').map { it.trim() }
    var index = lines.indexOfFirst { it.isNotEmpty() }
    if (index < 0) {
        return null
    }

    // A leading fenced code block is usually a signature sample rather than prose
    if (lines[index].startsWith("```")) {
        index++
        while (index < lines.size && !lines[index].startsWith("```")) {
            index++
        }
        index++
        while (index < lines.size && lines[index].isEmpty()) {
            index++
        }
    }

    val paragraph = StringBuilder()
    while (index < lines.size && lines[index].isNotEmpty()) {
        if (paragraph.isNotEmpty()) {
            paragraph.append(' ')
        }
        paragraph.append(lines[index])
        index++
    }

    var summary = paragraph.toString()
        .replace(Regex("```.*"), " ")
        .replace("`", "")
        .replace(Regex("""\[([^\]]+)]\([^)]*\)"""), "$1")
        .replace(Regex("\\s+"), " ")
        .trim()
    if (summary.isEmpty()) {
        return null
    }

    // Go doc comments start with the symbol name ("Command runs ..."), which reads
    // duplicated next to the label; strip it plus an optional connector
    val name = symbolName?.trim().orEmpty()
    if (name.isNotEmpty() && summary.startsWith(name, ignoreCase = true)) {
        summary = summary.substring(name.length).trimStart()
        val connector = summary.firstOrNull()
        if (connector != null && connector in "是:：-—–,，") {
            summary = summary.substring(1).trimStart()
        }
    }
    if (summary.isEmpty()) {
        return null
    }
    if (summary.length <= maxLength) {
        return summary
    }

    val cut = summary.take(maxLength).substringBeforeLast(' ').trimEnd()
    return (if (cut.isEmpty()) summary.take(maxLength) else cut) + "…"
}
