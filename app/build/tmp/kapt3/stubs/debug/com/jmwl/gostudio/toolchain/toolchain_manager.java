package com.jmwl.gostudio.toolchain;

/**
 * Go 工具链管理器（替代 XCode 的 cmake/ndk 探测）。
 *
 * Go/gopls 由 apt 装到 proot rootfs 内（/usr/local/go），不需要像 NDK 那样扫描多版本目录。
 * 探测逻辑：检查 rootfs 内 /usr/local/go/bin/go 是否存在，读 `go version` 输出取版本号。
 */
@kotlin.Metadata(mv = {2, 3, 0}, k = 1, xi = 48, d1 = {"\u0000B\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0003\n\u0002\u0010\u000e\n\u0002\b\u0005\n\u0002\u0010 \n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000b\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0010\"\n\u0002\b\u0003\n\u0002\u0010\u0002\n\u0002\b\u0005\b\u00c6\u0002\u0018\u00002\u00020\u0001B\t\b\u0002\u00a2\u0006\u0004\b\u0002\u0010\u0003J\u0016\u0010\f\u001a\u00020\u00052\u000e\b\u0002\u0010\r\u001a\b\u0012\u0004\u0012\u00020\u00050\u000bJ\b\u0010\u000e\u001a\u0004\u0018\u00010\u000fJ\u0006\u0010\u0010\u001a\u00020\u0011J\u0006\u0010\u0012\u001a\u00020\u0011J\u0006\u0010\u0013\u001a\u00020\u0011J\u000e\u0010\u0014\u001a\u00020\u00152\u0006\u0010\u0016\u001a\u00020\u0005J\f\u0010\u0017\u001a\b\u0012\u0004\u0012\u00020\u00050\u000bJ\f\u0010\u0018\u001a\b\u0012\u0004\u0012\u00020\u00050\u000bJ\f\u0010\u0019\u001a\b\u0012\u0004\u0012\u00020\u00050\u001aJ\u0006\u0010\u001b\u001a\u00020\u0011J\u0006\u0010\u001c\u001a\u00020\u0011J\u0006\u0010\u001d\u001a\u00020\u001eJ \u0010\u001f\u001a\u00020\u00052\u0006\u0010 \u001a\u00020\u00052\u0006\u0010!\u001a\u00020\u00052\u0006\u0010\"\u001a\u00020\u0005H\u0002R\u000e\u0010\u0004\u001a\u00020\u0005X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0006\u001a\u00020\u0005X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0007\u001a\u00020\u0005X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\b\u001a\u00020\u0005X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\t\u001a\u00020\u0005X\u0082T\u00a2\u0006\u0002\n\u0000R\u0014\u0010\n\u001a\b\u0012\u0004\u0012\u00020\u00050\u000bX\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006#"}, d2 = {"Lcom/jmwl/gostudio/toolchain/toolchain_manager;", "", "<init>", "()V", "PROOT_GOSTUDIO_HOME", "", "PROOT_GO_ROOT", "PROOT_GO_BIN", "PROOT_GOPLS", "PROOT_GOPATH_BIN", "base_proot_path", "", "proot_path", "extra_paths", "installed_go", "Lcom/jmwl/gostudio/toolchain/go_toolchain_info;", "is_go_installed", "", "is_gopls_installed", "is_git_installed", "project_environment", "Lcom/jmwl/gostudio/toolchain/project_toolchain_environment;", "project_path", "available_cmake_versions", "available_ndk_versions", "installed_ndk_version_keys", "", "is_cmake_installed", "is_ndk_installed", "cleanup_removed_toolchain_environment", "", "remove_block", "content", "start_marker", "end_marker", "app_debug"})
public final class toolchain_manager {
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String PROOT_GOSTUDIO_HOME = "/home/gostudio";
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String PROOT_GO_ROOT = "/usr/local/go";
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String PROOT_GO_BIN = "/usr/local/go/bin";
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String PROOT_GOPLS = "/usr/local/go/bin/gopls";
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String PROOT_GOPATH_BIN = "/home/go/bin";
    
    /**
     * guest 内的基础 PATH（标准 Linux 目录 + Go bin + GOPATH/bin）。
     * apt 装的 go 在 /usr/local/go/bin，gopls 经 `go install` 落到 $GOPATH/bin。
     */
    @org.jetbrains.annotations.NotNull()
    private static final java.util.List<java.lang.String> base_proot_path = null;
    @org.jetbrains.annotations.NotNull()
    public static final com.jmwl.gostudio.toolchain.toolchain_manager INSTANCE = null;
    
    private toolchain_manager() {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String proot_path(@org.jetbrains.annotations.NotNull()
    java.util.List<java.lang.String> extra_paths) {
        return null;
    }
    
    /**
     * 探测已安装的 Go 工具链（检查 rootfs 内 /usr/local/go/bin/go）。
     * 注意：版本号需 proot 执行 `go version` 才能拿到，这里只判断存在性，版本延迟到运行时。
     */
    @org.jetbrains.annotations.Nullable()
    public final com.jmwl.gostudio.toolchain.go_toolchain_info installed_go() {
        return null;
    }
    
    /**
     * Go 是否已安装。
     */
    public final boolean is_go_installed() {
        return false;
    }
    
    /**
     * gopls 是否已安装（/usr/local/go/bin/gopls 或 /home/go/bin/gopls）。
     */
    public final boolean is_gopls_installed() {
        return false;
    }
    
    /**
     * git 是否已安装（rootfs 内 /usr/bin/git）。
     */
    public final boolean is_git_installed() {
        return false;
    }
    
    /**
     * 组装项目构建环境。
     *
     * @param project_path 项目 host 路径（用于日志，Go 不像 cmake 需要工具链文件）
     */
    @org.jetbrains.annotations.NotNull()
    public final com.jmwl.gostudio.toolchain.project_toolchain_environment project_environment(@org.jetbrains.annotations.NotNull()
    java.lang.String project_path) {
        return null;
    }
    
    /**
     * 兼容旧调用（main_activity / main_tools_screen 引用）；GoStudio 不分版本，返回空列表。
     */
    @org.jetbrains.annotations.NotNull()
    public final java.util.List<java.lang.String> available_cmake_versions() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.util.List<java.lang.String> available_ndk_versions() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.util.Set<java.lang.String> installed_ndk_version_keys() {
        return null;
    }
    
    public final boolean is_cmake_installed() {
        return false;
    }
    
    public final boolean is_ndk_installed() {
        return false;
    }
    
    /**
     * 清理旧 .bashrc/.profile 里的 XCode 工具链环境块（迁移期兼容）。
     */
    public final void cleanup_removed_toolchain_environment() {
    }
    
    private final java.lang.String remove_block(java.lang.String content, java.lang.String start_marker, java.lang.String end_marker) {
        return null;
    }
}