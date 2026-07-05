package com.xc.code.ui.screens.main

enum class toolchain_action {
    INSTALL_CMAKE,
    INSTALL_CMAKE_ARCHIVE,
    UNINSTALL_CMAKE,
    INSTALL_NDK_URL,
    INSTALL_NDK_ARCHIVE,
    UNINSTALL_NDK
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
