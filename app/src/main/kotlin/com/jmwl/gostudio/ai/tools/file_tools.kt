package com.jmwl.gostudio.ai.tools

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.io.File

/** 工具输出截断阈值（防上下文溢出，仿 pi 的 50KB / 2000 行） */
private const val MAX_OUTPUT_BYTES = 50_000
private const val MAX_OUTPUT_LINES = 2000

/** 路径越界提示 */
private const val PATH_DENY = "路径越界：只允许访问项目目录内的文件"

/** 把模型给的路径（相对或绝对）解析到项目根下，越界返回 null */
internal fun resolve_in_project(project_root: File, path: String): File? {
    val cleaned = path.trim().removePrefix("file://")
    val target = if (cleaned.startsWith("/")) File(cleaned) else File(project_root, cleaned)
    val canonical = runCatching { target.canonicalFile }.getOrNull() ?: return null
    val rootCanonical = project_root.canonicalFile
    // 必须在项目根之内（含根本身）
    return if (canonical.path.startsWith(rootCanonical.path)) canonical else null
}

/** 截断长输出 */
internal fun truncate_output(text: String): String {
    val lines = text.lines()
    if (lines.size > MAX_OUTPUT_LINES) {
        return lines.take(MAX_OUTPUT_LINES).joinToString("\n") +
            "\n\n... (输出已截断，共 ${lines.size} 行，仅显示前 $MAX_OUTPUT_LINES 行)"
    }
    val bytes = text.toByteArray(Charsets.UTF_8)
    if (bytes.size > MAX_OUTPUT_BYTES) {
        return text.take(MAX_OUTPUT_BYTES) + "\n\n... (输出已截断，超过 ${MAX_OUTPUT_BYTES / 1024}KB)"
    }
    return text
}

// ============ read 工具 ============
class read_tool(private val project_root: File) : ai_tool {
    override val name = "read"
    override val description = "读取项目内文件的内容。path 为相对项目根的路径或绝对路径。"
    override val parameters_schema: JsonObject = JsonParser.parseString("""
        {"type":"object","properties":{"path":{"type":"string","description":"要读取的文件路径（相对项目根或绝对路径）"}},"required":["path"]}
    """.trimIndent()).asJsonObject

    override suspend fun execute(params: JsonObject): String {
        val path = params.string_or("path")
        if (path.isBlank()) return "缺少 path 参数"
        val file = resolve_in_project(project_root, path) ?: return PATH_DENY
        if (!file.exists()) return "文件不存在: $path"
        if (file.isDirectory) return "目标是目录而非文件: $path"
        return truncate_output(file.readText())
    }
}

// ============ write 工具 ============
class write_tool(private val project_root: File) : ai_tool {
    override val name = "write"
    override val description = "写入或创建项目内文件（覆盖整个文件内容）。path 为路径，content 为新内容。"
    override val parameters_schema: JsonObject = JsonParser.parseString("""
        {"type":"object","properties":{"path":{"type":"string","description":"文件路径（相对项目根或绝对路径）"},"content":{"type":"string","description":"写入的完整文件内容"}},"required":["path","content"]}
    """.trimIndent()).asJsonObject

    override suspend fun execute(params: JsonObject): String {
        val path = params.string_or("path")
        val content = params.string_or("content")
        if (path.isBlank()) return "缺少 path 参数"
        val file = resolve_in_project(project_root, path) ?: return PATH_DENY
        file.parentFile?.mkdirs()
        val isNew = !file.exists()
        file.writeText(content)
        return (if (isNew) "已创建文件" else "已写入文件") + ": ${file.relativeTo(project_root).path}（${content.length} 字符）"
    }
}

// ============ edit 工具（精确替换代码片段，仿 pi）============
class edit_tool(private val project_root: File) : ai_tool {
    override val name = "edit"
    override val description = "修改项目内已有文件的部分内容。找到 oldText 替换为 newText，oldText 必须在文件中唯一存在。"
    override val parameters_schema: JsonObject = JsonParser.parseString("""
        {"type":"object","properties":{"path":{"type":"string","description":"文件路径"},"oldText":{"type":"string","description":"要被替换的原文（必须精确匹配且唯一）"},"newText":{"type":"string","description":"替换后的新文本"}},"required":["path","oldText","newText"]}
    """.trimIndent()).asJsonObject

    override suspend fun execute(params: JsonObject): String {
        val path = params.string_or("path")
        val oldText = params.string_or("oldText")
        val newText = params.string_or("newText")
        if (path.isBlank()) return "缺少 path 参数"
        if (oldText.isEmpty()) return "缺少 oldText 参数"
        val file = resolve_in_project(project_root, path) ?: return PATH_DENY
        if (!file.isFile) return "文件不存在: $path"
        val content = file.readText()
        // 统计 oldText 出现次数（用字面量转义，避免正则元字符干扰）
        val escaped = Regex.escape(oldText)
        val count = Regex(escaped).findAll(content).count()
        when {
            count == 0 -> return "未找到要替换的文本，请确认 oldText 是否精确匹配"
            count > 1 -> return "oldText 在文件中出现了 $count 次，请提供更长的上下文使其唯一"
        }
        // 用字面量替换首个（前面 count 检查保证唯一）
        file.writeText(content.replaceFirst(oldText.toRegex(RegexOption.LITERAL), newText))
        return "已修改: ${file.relativeTo(project_root).path}"
    }
}

// ============ ls 工具 ============
class ls_tool(private val project_root: File) : ai_tool {
    override val name = "ls"
    override val description = "列出项目内某目录的文件和子目录。path 默认为项目根。"
    override val parameters_schema: JsonObject = JsonParser.parseString("""
        {"type":"object","properties":{"path":{"type":"string","description":"要列出的目录路径，默认项目根"}}}
    """.trimIndent()).asJsonObject

    override suspend fun execute(params: JsonObject): String {
        val path = params.string_or("path", ".")
        val dir = resolve_in_project(project_root, path) ?: return PATH_DENY
        if (!dir.exists()) return "目录不存在: $path"
        if (!dir.isDirectory) return "不是目录: $path"
        val entries = dir.listFiles()?.toList()?.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
            ?: return "无法读取目录: $path"
        if (entries.isEmpty()) return "(空目录)"
        val sb = StringBuilder()
        sb.appendLine("${dir.relativeTo(project_root).path.ifBlank { "." }}/")
        for (e in entries) {
            val prefix = if (e.isDirectory) "[目录] " else "      "
            val size = if (e.isFile) "  (${format_size(e.length())})" else ""
            sb.appendLine("  $prefix${e.name}$size")
        }
        return truncate_output(sb.toString())
    }

    private fun format_size(bytes: Long): String = when {
        bytes < 1024 -> "${bytes}B"
        bytes < 1024 * 1024 -> "${bytes / 1024}KB"
        else -> "${bytes / 1024 / 1024}MB"
    }
}

// ============ grep 工具 ============
class grep_tool(private val project_root: File) : ai_tool {
    override val name = "grep"
    override val description = "在项目文件中搜索文本（支持正则）。pattern 为搜索内容，path 限定搜索目录（默认项目根）。"
    override val parameters_schema: JsonObject = JsonParser.parseString("""
        {"type":"object","properties":{"pattern":{"type":"string","description":"搜索的正则或纯文本"},"path":{"type":"string","description":"搜索范围目录，默认项目根"},"include":{"type":"string","description":"文件名 glob 过滤，如 *.go"}},"required":["pattern"]}
    """.trimIndent()).asJsonObject

    override suspend fun execute(params: JsonObject): String {
        val pattern = params.string_or("pattern")
        if (pattern.isBlank()) return "缺少 pattern 参数"
        val searchRoot = resolve_in_project(project_root, params.string_or("path", ".")) ?: return PATH_DENY
        if (!searchRoot.exists()) return "搜索目录不存在"
        val includeGlob = params.string_or("include", "*.go")
        val regex = runCatching { Regex(pattern) }.getOrElse { return "正则无效: ${it.message}" }
        val results = mutableListOf<String>()
        searchRoot.walkTopDown()
            .filter { it.isFile && !it.path.contains("/.git/") }
            .filter { includeGlob == "*" || it.name matchesGlob includeGlob }
            .forEach { file ->
                var matched = false
                file.useLines { lines ->
                    lines.forEachIndexed { i, line ->
                        if (regex.containsMatchIn(line)) {
                            if (!matched) {
                                results.add("${file.relativeTo(project_root).path}:")
                                matched = true
                            }
                            results.add("  ${i + 1}: ${line.trim().take(200)}")
                        }
                    }
                }
            }
        if (results.isEmpty()) return "未找到匹配"
        results.add(0, "找到 ${results.count { it.endsWith(":") && !it.startsWith("  ") }} 个文件：")
        return truncate_output(results.joinToString("\n"))
    }
}

/** 简单 glob 匹配（仅支持 * 通配） */
private infix fun String.matchesGlob(glob: String): Boolean {
    if (!glob.contains("*")) return this == glob
    val regexStr = glob.replace(".", "\\.").replace("*", ".*")
    return this.matches(Regex("^$regexStr$"))
}
