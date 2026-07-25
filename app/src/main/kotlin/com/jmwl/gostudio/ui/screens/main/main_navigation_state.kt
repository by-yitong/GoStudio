package com.jmwl.gostudio.ui.screens.main

enum class toolchain_action {
    INSTALL_GO,
    INSTALL_GOPLS,
    INSTALL_GIT,
    INSTALL_GARBLE
}

data class toolchain_trigger(
    val title: String,
    val action: toolchain_action,
    val source: String = "",
    val version: String = "",
    val sha256: String = ""
)

data class toolchain_custom_install_request(
    val title: String,
    val on_install: (String) -> Unit
)
