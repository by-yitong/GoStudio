package com.jmwl.gostudio.ai.tools

import com.google.gson.JsonObject
import com.jmwl.gostudio.toolchain.proot_manager
import java.io.File

/**
 * bash 工具：在 proot rootfs 内执行 shell 命令（go build / go vet / go mod tidy 等）。
 *
 * 工作目录默认项目根，带 Go 工具链环境（GOROOT/GOPATH/GOPROXY 等）。
 * 输出按行收集后截断，防止上下文溢出。
 */
class bash_tool(
    private val project_root: File,
    private val extra_environment: Map<String, String> = emptyMap()
) : ai_tool {
    override val name = "bash"
    override val description = "在项目目录内执行 shell 命令（如 go build、go vet、go test、go mod tidy、ls 等）。命令在 Linux rootfs 内运行，自带 Go 工具链。"
    override val parameters_schema: JsonObject = com.google.gson.JsonParser.parseString("""
        {"type":"object","properties":{"command":{"type":"string","description":"要执行的 shell 命令（如 go vet ./...）"}},"required":["command"]}
    """.trimIndent()).asJsonObject

    override suspend fun execute(params: JsonObject): String {
        val command = params.string_or("command")
        if (command.isBlank()) return "缺少 command 参数"
        val output = StringBuilder()
        val success = proot_manager.execute_command_with_environment(
            command = command,
            working_dir = project_root.absolutePath,
            extra_environment = extra_environment,
            on_log = { line -> output.appendLine(line) }
        )
        val result = output.toString().trim()
        val truncated = if (result.isEmpty()) "(命令无输出)" else truncate_output(result)
        return "$truncated\n\n[退出码: ${if (success) "0 (成功)" else "非0 (失败)"}]"
    }
}
