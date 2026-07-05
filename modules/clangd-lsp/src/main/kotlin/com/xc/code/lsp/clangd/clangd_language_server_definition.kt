package com.xc.code.lsp.clangd

import io.github.rosemoe.sora.lsp.client.connection.StreamConnectionProvider
import io.github.rosemoe.sora.lsp.client.languageserver.LspFeature
import io.github.rosemoe.sora.lsp.client.languageserver.serverdefinition.CustomLanguageServerDefinition
import io.github.rosemoe.sora.lsp.client.languageserver.serverdefinition.LanguageServerDefinition

private val clangd_extensions = listOf(
    "c",
    "cc",
    "cpp",
    "cxx",
    "c++",
    "h",
    "hh",
    "hpp",
    "hxx",
    "h++",
    "m",
    "mm"
)

fun create_clangd_language_server_definitions(
    config_factory: (working_dir: String) -> clangd_lsp_config,
    disabled_features: Set<LspFeature> = emptySet()
): List<LanguageServerDefinition> {
    return clangd_extensions.map { extension ->
        object : CustomLanguageServerDefinition(
            ext = extension,
            serverConnectProvider = CustomLanguageServerDefinition.ServerConnectProvider { working_dir ->
                create_clangd_connection_provider(config_factory(working_dir))
            },
            name = clangd_server_name,
            expectedCapabilitiesOverride = null,
            extensionsOverride = listOf(extension)
        ) {
            override val disabledFeatures: Set<LspFeature> = disabled_features
        }
    }
}

fun create_clangd_connection_provider(config: clangd_lsp_config): StreamConnectionProvider {
    return clangd_stream_connection_provider(config)
}

const val clangd_server_name = "clangd"
