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

rootProject.name = "GoStudio"

include(
    ":app",
    ":shell",
    ":modules:editor-core",
    ":modules:project-file-tree",
    ":modules:sora-editor",
    ":modules:sora-editor-lsp",
    ":modules:sora-language-textmate",
    ":modules:sora-oniguruma-native",
    ":modules:gopls-lsp",
    ":modules:toolchain-runtime",
    ":modules:terminal-view",
    ":modules:terminal-emulator"
)
