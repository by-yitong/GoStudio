package com.xc.code.toolchain.runtime

import java.io.File

data class proot_bind_mount(
    val source: File,
    val target: String? = null
) {
    init {
        require(target == null || target.startsWith("/")) { "Bind target must be absolute: $target" }
    }

    fun as_argument(): String = if (target.isNullOrBlank()) {
        source.absolutePath
    } else {
        "${source.absolutePath}:${target.trimEnd('/')}"
    }
}
