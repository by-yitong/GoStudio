package com.jmwl.gostudio.plugins

import org.json.JSONObject
import java.io.File

/**
 * 插件 manifest 数据模型。
 *
 * 一个插件是 plugins/<id>/ 目录，目录内必须含 manifest.json。
 * 其余目录按能力可选存在：
 * - skills/    AI 技能（每个子目录一个 SKILL.md）
 * - templates/ 项目模板（预留）
 * - themes/    编辑器主题（预留）
 */
data class plugin_manifest(
    val id: String,                 // 唯一 id，形如 com.example.my-plugin
    val name: String,               // 显示名
    val version: String,            // 语义化版本
    val description: String = "",   // 可选描述
    val author: String = "",        // 可选作者
    val min_app_version: Int = 0    // 最低宿主版本（预留）
) {
    companion object {
        private val id_pattern = Regex("^[a-zA-Z][a-zA-Z0-9_]*(\\.[a-zA-Z][a-zA-Z0-9_]*)+$")
        private val version_pattern = Regex("^\\d+\\.\\d+\\.\\d+$")

        /** 从 manifest.json 文件解析；失败返回 null */
        fun from_file(file: File): plugin_manifest? {
            val text = runCatching { file.readText() }.getOrNull() ?: return null
            return from_json(text)
        }

        /** 从 JSON 文本解析并校验 */
        fun from_json(text: String): plugin_manifest? {
            return runCatching {
                val json = JSONObject(text)
                val id = json.optString("id").trim()
                val name = json.optString("name").trim()
                val version = json.optString("version", "1.0.0").trim()
                if (id.isEmpty() || name.isEmpty()) return null
                if (!id_pattern.matches(id)) return null
                if (!version_pattern.matches(version)) return null
                plugin_manifest(
                    id = id,
                    name = name,
                    version = version,
                    description = json.optString("description").trim(),
                    author = json.optString("author").trim(),
                    min_app_version = json.optInt("min_app_version", 0)
                )
            }.getOrNull()
        }
    }
}

/** 扫描到的插件实例：manifest + 目录 + 能力标记 */
data class plugin_instance(
    val manifest: plugin_manifest,
    val dir: File,
    val enabled: Boolean
) {
    val id: String get() = manifest.id

    /** 插件提供的能力列表（用于 UI 展示） */
    fun capabilities(): List<String> {
        val caps = mutableListOf<String>()
        if (File(dir, "skills").isDirectory) caps.add("AI 技能")
        if (File(dir, "templates").isDirectory) caps.add("项目模板")
        if (File(dir, "themes").isDirectory) caps.add("主题")
        return caps
    }

    /** 该插件的 skills 目录（存在才返回） */
    fun skill_dir(): File? = File(dir, "skills").takeIf { it.isDirectory }
}
