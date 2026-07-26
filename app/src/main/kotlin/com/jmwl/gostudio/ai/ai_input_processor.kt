package com.jmwl.gostudio.ai

import com.jmwl.gostudio.ai.skills.ai_skill_manager
import java.io.File

/**
 * 处理用户输入的增强：@文件引用、/命令模板、/skill 激活。
 *
 * @引用：输入里的 `@path`（相对项目根或绝对）会被解析，文件内容注入到消息里
 * /命令：输入 `/name args` 展开为 prompt 模板（.ai/prompts/name.md）
 * /skill:name：激活 skill，把 SKILL.md 全文注入
 *
 * @return 处理后的用户消息文本（已展开引用/模板）；若返回 null 表示是纯命令无需发模型
 */
class ai_input_processor(
    private val project_dir: File?,
    private val skill_manager: ai_skill_manager?,
    private val global_prompts_dir: File,
    private val project_prompts_dir: File?
) {
    /**
     * 处理用户输入，返回最终发给模型的文本。
     * 可能展开 @引用、/命令模板、/skill 激活。
     */
    fun process(raw_input: String): String {
        val trimmed = raw_input.trim()

        // /skill:name → 激活 skill
        if (trimmed.startsWith("/skill:")) {
            val skill_name = trimmed.removePrefix("/skill:").trim()
            val activated = skill_manager?.activate(skill_name)
            return if (activated != null) {
                "（已激活技能 $skill_name，以下是该技能的指令，请遵循）\n\n$activated\n\n---\n用户附加说明：${trimmed.substringAfter(' ', "").ifBlank { "(无)" }}"
            } else {
                "⚠️ 未找到技能「$skill_name」"
            }
        }

        // /命令 → prompt 模板
        if (trimmed.startsWith("/") && !trimmed.startsWith("/")) {
            val parts = trimmed.removePrefix("/").split(Regex("\\s+"), limit = 2)
            val template_name = parts[0]
            val args = parts.getOrNull(1) ?: ""
            val expanded = expand_template(template_name, args)
            if (expanded != null) return expanded
        }

        // @文件引用：扫描 @path，读文件内容注入
        return expand_file_mentions(trimmed)
    }

    /** 展开 @文件引用。@main.go @internal/handler.go 等 */
    private fun expand_file_mentions(text: String): String {
        if (project_dir == null) return text
        val mention_regex = Regex("@([\\w./\\-]+\\.\\w+(?:/[\\w./\\-]+\\.\\w+)*)")
        val mentions = mention_regex.findAll(text).map { it.groupValues[1] }.distinct().toList()
        if (mentions.isEmpty()) return text

        val sb = StringBuilder(text)
        sb.append("\n\n---\n引用的文件内容：\n")
        var any = false
        for (path in mentions) {
            val file = if (path.startsWith("/")) File(path) else File(project_dir, path)
            if (!file.isFile) {
                sb.append("\n[$path]: 文件不存在\n")
                continue
            }
            val content = runCatching { file.readText() }.getOrElse { "读取失败: ${it.message}" }
            val truncated = if (content.length > 8000) content.take(8000) + "\n...(截断)" else content
            sb.append("\n=== $path ===\n```\n$truncated\n```\n")
            any = true
        }
        return if (any) sb.toString() else text
    }

    /** 展开 prompt 模板 /name */
    private fun expand_template(name: String, args: String): String? {
        // 查找顺序：项目 > 全局
        val candidates = listOfNotNull(project_prompts_dir, global_prompts_dir)
        for (dir in candidates) {
            val tpl = File(dir, "$name.md")
            if (tpl.isFile) {
                val body = tpl.readText().let { strip_frontmatter(it) }
                // 替换 $1 $@ $ARGUMENTS 等占位符
                return substitute_placeholders(body, args)
            }
        }
        return null
    }

    /** 去掉模板的 frontmatter，返回正文 */
    private fun strip_frontmatter(text: String): String {
        if (!text.startsWith("---")) return text
        val end = text.indexOf("\n---", 3)
        return if (end < 0) text else text.substring(end + 4).trim()
    }

    /** 替换 $1 $@ $ARGUMENTS 占位符 */
    private fun substitute_placeholders(body: String, args: String): String {
        val parts = args.split(Regex("\\s+")).filter { it.isNotBlank() }
        var result = body
        result = result.replace("$@", args).replace("\$ARGUMENTS", args)
        for ((i, p) in parts.withIndex()) {
            result = result.replace("\$${i + 1}", p)
        }
        return result
    }
}

/**
 * 读取项目/全局的上下文文件，注入 system prompt。
 * 参考 pi 的 AGENTS.md / .ai/SYSTEM.md / .ai/APPEND_SYSTEM.md 机制：
 * - AGENTS.md（项目根，向上找到 git 根）：项目规范
 * - .ai/SYSTEM.md：完全替换默认 system prompt（少用）
 * - .ai/APPEND_SYSTEM.md：追加到默认 system prompt
 */
fun read_context_files(project_dir: File?): String {
    if (project_dir == null) return ""
    val sb = StringBuilder()

    // AGENTS.md（项目根，向上查到 git 根）
    find_agents_md(project_dir)?.let { (file, content) ->
        sb.appendLine("## 项目规范（AGENTS.md）")
        sb.appendLine(content.trim().take(4000))
        sb.appendLine()
    }

    // .ai/APPEND_SYSTEM.md
    File(project_dir, ".ai/APPEND_SYSTEM.md").takeIf { it.isFile }?.let {
        sb.appendLine("## 用户追加的指令（.ai/APPEND_SYSTEM.md）")
        sb.appendLine(it.readText().trim().take(4000))
        sb.appendLine()
    }

    return sb.toString().trimEnd()
}

/** 从项目目录向上找 AGENTS.md 或 CLAUDE.md（到 git 根为止） */
private fun find_agents_md(start: File): Pair<File, String>? {
    var dir: File? = start
    while (dir != null && dir.isDirectory) {
        val agents = File(dir, "AGENTS.md")
        if (agents.isFile) return agents to agents.readText()
        val claude = File(dir, "CLAUDE.md")
        if (claude.isFile) return claude to claude.readText()
        // 到 git 根停止
        if (File(dir, ".git").isDirectory) return null
        dir = dir.parentFile
    }
    return null
}
