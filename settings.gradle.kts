pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
    }
}

rootProject.name = "XCode"

include(
    ":app",
    ":modules:editor-core",
    ":modules:project-file-tree",
    ":modules:sora-editor",
    ":modules:sora-editor-lsp",
    ":modules:sora-language-textmate",
    ":modules:sora-oniguruma-native",
    ":modules:clangd-lsp",
    ":modules:toolchain-runtime",
    ":modules:terminal-view",
    ":modules:terminal-emulator"
)
