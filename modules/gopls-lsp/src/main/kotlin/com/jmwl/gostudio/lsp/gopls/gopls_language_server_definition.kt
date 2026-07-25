package com.jmwl.gostudio.lsp.gopls

import io.github.rosemoe.sora.lsp.client.connection.StreamConnectionProvider
import io.github.rosemoe.sora.lsp.client.languageserver.LspFeature
import io.github.rosemoe.sora.lsp.client.languageserver.serverdefinition.CustomLanguageServerDefinition
import io.github.rosemoe.sora.lsp.client.languageserver.serverdefinition.LanguageServerDefinition

/** gopls 处理的文件扩展名（Go 源码 + go.mod）。 */
private val gopls_extensions = listOf("go", "mod")

/**
 * 为 Go 扩展名创建 gopls 的 LanguageServerDefinition 列表。
 * 每个 .go/.mod 文件会通过 [config_factory] 获得 gopls 连接。
 */
fun create_gopls_language_server_definitions(
    config_factory: (working_dir: String) -> gopls_lsp_config,
    disabled_features: Set<LspFeature> = emptySet()
): List<LanguageServerDefinition> {
    return gopls_extensions.map { extension ->
        object : CustomLanguageServerDefinition(
            ext = extension,
            serverConnectProvider = CustomLanguageServerDefinition.ServerConnectProvider { working_dir ->
                create_gopls_connection_provider(config_factory(working_dir))
            },
            name = gopls_server_name,
            expectedCapabilitiesOverride = null,
            extensionsOverride = listOf(extension)
        ) {
            override val disabledFeatures: Set<LspFeature> = disabled_features
        }
    }
}

fun create_gopls_connection_provider(config: gopls_lsp_config): StreamConnectionProvider {
    return gopls_stream_connection_provider(config)
}

const val gopls_server_name = "gopls"
