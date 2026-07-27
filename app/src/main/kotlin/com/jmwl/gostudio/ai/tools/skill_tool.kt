package com.jmwl.gostudio.ai.tools

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.jmwl.gostudio.ai.skills.ai_skill_manager
import java.io.File

/**
 * create_skill 工具：让 AI 在对话中自动创建技能。
 * 调用 skill_manager.create_skill 写入全局技能目录。
 */
class create_skill_tool(private val skill_manager: ai_skill_manager) : ai_tool {
    override val name = "create_skill"
    override val description = "创建一个新的技能（Skill）。技能是一组可复用的指令，写好后 AI 在合适场景会自动参考。" +
        "name 用英文短横线命名（如 code-review），description 说明何时触发，content 是详细指令正文。"
    override val parameters_schema: JsonObject = JsonParser.parseString("""
        {"type":"object","properties":{"name":{"type":"string","description":"技能名称（英文，如 code-review）"},"description":{"type":"string","description":"技能描述：什么场景下触发"},"content":{"type":"string","description":"技能指令正文（Markdown）"}},"required":["name","description","content"]}
    """.trimIndent()).asJsonObject

    override suspend fun execute(params: JsonObject): String {
        val name = params.get("name")?.asString ?: return "缺少 name"
        val desc = params.get("description")?.asString ?: return "缺少 description"
        val content = params.get("content")?.asString ?: return "缺少 content"
        return if (skill_manager.create_skill(name, desc, content)) {
            "✅ 技能 '$name' 创建成功。AI 现在会在合适场景参考它。"
        } else {
            "❌ 技能创建失败（名称无效或写入出错）"
        }
    }
}
