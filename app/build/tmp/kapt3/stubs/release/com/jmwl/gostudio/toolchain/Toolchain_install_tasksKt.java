package com.jmwl.gostudio.toolchain;

@kotlin.Metadata(mv = {2, 3, 0}, k = 2, xi = 48, d1 = {"\u0000\u001e\n\u0000\n\u0002\u0010\u000b\n\u0000\n\u0002\u0018\u0002\n\u0002\u0010\u000e\n\u0002\u0010\u0002\n\u0000\n\u0002\u0010\b\n\u0002\b/\u001a6\u0010\u0000\u001a\u00020\u00012\u0012\u0010\u0002\u001a\u000e\u0012\u0004\u0012\u00020\u0004\u0012\u0004\u0012\u00020\u00050\u00032\u0012\u0010\u0006\u001a\u000e\u0012\u0004\u0012\u00020\u0007\u0012\u0004\u0012\u00020\u00050\u0003H\u0086@\u00a2\u0006\u0002\u0010\b\u001aN\u0010\t\u001a\u00020\u00012\u0006\u0010\n\u001a\u00020\u00042\u0006\u0010\u000b\u001a\u00020\u00042\u0006\u0010\f\u001a\u00020\u00042\u0012\u0010\u0002\u001a\u000e\u0012\u0004\u0012\u00020\u0004\u0012\u0004\u0012\u00020\u00050\u00032\u0012\u0010\u0006\u001a\u000e\u0012\u0004\u0012\u00020\u0007\u0012\u0004\u0012\u00020\u00050\u0003H\u0086@\u00a2\u0006\u0002\u0010\r\u001a>\u0010\u000e\u001a\u00020\u00012\u0006\u0010\u000f\u001a\u00020\u00042\u0012\u0010\u0002\u001a\u000e\u0012\u0004\u0012\u00020\u0004\u0012\u0004\u0012\u00020\u00050\u00032\u0012\u0010\u0006\u001a\u000e\u0012\u0004\u0012\u00020\u0007\u0012\u0004\u0012\u00020\u00050\u0003H\u0086@\u00a2\u0006\u0002\u0010\u0010\u001a>\u0010\u0011\u001a\u00020\u00012\u0006\u0010\n\u001a\u00020\u00042\u0012\u0010\u0002\u001a\u000e\u0012\u0004\u0012\u00020\u0004\u0012\u0004\u0012\u00020\u00050\u00032\u0012\u0010\u0006\u001a\u000e\u0012\u0004\u0012\u00020\u0007\u0012\u0004\u0012\u00020\u00050\u0003H\u0086@\u00a2\u0006\u0002\u0010\u0010\u001aP\u0010\u0012\u001a\u00020\u00012\u0006\u0010\n\u001a\u00020\u00042\u0006\u0010\u000b\u001a\u00020\u00042\b\b\u0002\u0010\f\u001a\u00020\u00042\u0012\u0010\u0002\u001a\u000e\u0012\u0004\u0012\u00020\u0004\u0012\u0004\u0012\u00020\u00050\u00032\u0012\u0010\u0006\u001a\u000e\u0012\u0004\u0012\u00020\u0007\u0012\u0004\u0012\u00020\u00050\u0003H\u0086@\u00a2\u0006\u0002\u0010\r\u001a>\u0010\u0013\u001a\u00020\u00012\u0006\u0010\u000f\u001a\u00020\u00042\u0012\u0010\u0002\u001a\u000e\u0012\u0004\u0012\u00020\u0004\u0012\u0004\u0012\u00020\u00050\u00032\u0012\u0010\u0006\u001a\u000e\u0012\u0004\u0012\u00020\u0007\u0012\u0004\u0012\u00020\u00050\u0003H\u0086@\u00a2\u0006\u0002\u0010\u0010\u001a>\u0010\u0014\u001a\u00020\u00012\u0006\u0010\n\u001a\u00020\u00042\u0012\u0010\u0002\u001a\u000e\u0012\u0004\u0012\u00020\u0004\u0012\u0004\u0012\u00020\u00050\u00032\u0012\u0010\u0006\u001a\u000e\u0012\u0004\u0012\u00020\u0007\u0012\u0004\u0012\u00020\u00050\u0003H\u0086@\u00a2\u0006\u0002\u0010\u0010\u001a$\u0010\u0015\u001a\u00020\u00042\u0006\u0010\u000f\u001a\u00020\u00042\b\u0010\u0016\u001a\u0004\u0018\u00010\u00042\b\b\u0002\u0010\f\u001a\u00020\u0004H\u0002\u001a>\u0010\u0017\u001a\u00020\u00012\u0006\u0010\u0018\u001a\u00020\u00042\u0012\u0010\u0002\u001a\u000e\u0012\u0004\u0012\u00020\u0004\u0012\u0004\u0012\u00020\u00050\u00032\u0012\u0010\u0006\u001a\u000e\u0012\u0004\u0012\u00020\u0007\u0012\u0004\u0012\u00020\u00050\u0003H\u0082@\u00a2\u0006\u0002\u0010\u0010\u001a*\u0010\u0019\u001a\u00020\u00012\u0006\u0010\u001a\u001a\u00020\u00042\u0012\u0010\u0002\u001a\u000e\u0012\u0004\u0012\u00020\u0004\u0012\u0004\u0012\u00020\u00050\u0003H\u0082@\u00a2\u0006\u0002\u0010\u001b\u001aN\u0010\u001c\u001a\u00020\u00012\u0006\u0010\u001d\u001a\u00020\u00042\u0006\u0010\u0018\u001a\u00020\u00042\u0006\u0010\u001e\u001a\u00020\u00042\u0012\u0010\u0002\u001a\u000e\u0012\u0004\u0012\u00020\u0004\u0012\u0004\u0012\u00020\u00050\u00032\u0012\u0010\u0006\u001a\u000e\u0012\u0004\u0012\u00020\u0007\u0012\u0004\u0012\u00020\u00050\u0003H\u0082@\u00a2\u0006\u0002\u0010\r\u001a>\u0010\u001f\u001a\u00020\u00012\u0006\u0010\u0018\u001a\u00020\u00042\u0012\u0010\u0002\u001a\u000e\u0012\u0004\u0012\u00020\u0004\u0012\u0004\u0012\u00020\u00050\u00032\u0012\u0010\u0006\u001a\u000e\u0012\u0004\u0012\u00020\u0007\u0012\u0004\u0012\u00020\u00050\u0003H\u0082@\u00a2\u0006\u0002\u0010\u0010\u001a\u0018\u0010 \u001a\u00020\u00042\u0006\u0010\u000b\u001a\u00020\u00042\u0006\u0010\f\u001a\u00020\u0004H\u0002\u001a,\u0010!\u001a\u00020\u00042\u0006\u0010\u000f\u001a\u00020\u00042\u0006\u0010\"\u001a\u00020\u00042\b\u0010\u0016\u001a\u0004\u0018\u00010\u00042\b\b\u0002\u0010\f\u001a\u00020\u0004H\u0002\u001a$\u0010#\u001a\u00020\u00052\u0006\u0010$\u001a\u00020\u00042\u0012\u0010\u0006\u001a\u000e\u0012\u0004\u0012\u00020\u0007\u0012\u0004\u0012\u00020\u00050\u0003H\u0002\u001a\u0010\u0010%\u001a\u00020\u00042\u0006\u0010&\u001a\u00020\u0004H\u0002\u001a\u0010\u0010\'\u001a\u00020\u00042\u0006\u0010\u000b\u001a\u00020\u0004H\u0002\u001a\u0010\u0010(\u001a\u00020\u00042\u0006\u0010)\u001a\u00020\u0004H\u0002\u001a\u0010\u0010*\u001a\u00020\u00042\u0006\u0010+\u001a\u00020\u0004H\u0002\u001a\u0010\u0010,\u001a\u00020\u00042\u0006\u0010+\u001a\u00020\u0004H\u0002\u001a\u0010\u0010-\u001a\u00020\u00042\u0006\u0010+\u001a\u00020\u0004H\u0002\u001a\u0010\u0010.\u001a\u00020\u00042\u0006\u0010+\u001a\u00020\u0004H\u0002\u001a\f\u0010/\u001a\u00020\u0004*\u00020\u0004H\u0002\u001a*\u00100\u001a\u00020\u00052\u0012\u0010\u0002\u001a\u000e\u0012\u0004\u0012\u00020\u0004\u0012\u0004\u0012\u00020\u00050\u00032\u0006\u00101\u001a\u00020\u0004H\u0082@\u00a2\u0006\u0002\u00102\u001a*\u00103\u001a\u00020\u00052\u0012\u0010\u0006\u001a\u000e\u0012\u0004\u0012\u00020\u0007\u0012\u0004\u0012\u00020\u00050\u00032\u0006\u00104\u001a\u00020\u0007H\u0082@\u00a2\u0006\u0002\u00105\u00a8\u00066"}, d2 = {"install_cmake_tool", "", "on_log", "Lkotlin/Function1;", "", "", "on_progress", "", "(Lkotlin/jvm/functions/Function1;Lkotlin/jvm/functions/Function1;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "install_cmake_from_url", "version", "url", "sha256", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Lkotlin/jvm/functions/Function1;Lkotlin/jvm/functions/Function1;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "install_cmake_from_archive", "archive_path", "(Ljava/lang/String;Lkotlin/jvm/functions/Function1;Lkotlin/jvm/functions/Function1;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "uninstall_cmake_tool", "install_ndk_from_url", "install_ndk_from_archive", "uninstall_ndk_tool", "create_cmake_install_command", "download_url", "run_cmake_install_task", "command", "install_bundled_ninja", "cmake_target", "(Ljava/lang/String;Lkotlin/jvm/functions/Function1;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "run_proot_toolchain_task", "start_log", "success_log", "run_ndk_install_task", "create_download_block", "create_ndk_install_command", "target_name", "update_ndk_progress_from_log", "line", "host_path_to_proot_path", "path", "archive_name_from_url", "strip_archive_extension", "name", "normalize_cmake_install_name", "value", "normalize_ndk_install_name", "normalize_version_key", "shell_quote", "with_shell_dollar", "emit_log", "message", "(Lkotlin/jvm/functions/Function1;Ljava/lang/String;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "emit_progress", "progress", "(Lkotlin/jvm/functions/Function1;ILkotlin/coroutines/Continuation;)Ljava/lang/Object;", "app_release"})
public final class Toolchain_install_tasksKt {
    
    @org.jetbrains.annotations.Nullable()
    public static final java.lang.Object install_cmake_tool(@org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function1<? super java.lang.String, kotlin.Unit> on_log, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function1<? super java.lang.Integer, kotlin.Unit> on_progress, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.lang.Boolean> $completion) {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public static final java.lang.Object install_cmake_from_url(@org.jetbrains.annotations.NotNull()
    java.lang.String version, @org.jetbrains.annotations.NotNull()
    java.lang.String url, @org.jetbrains.annotations.NotNull()
    java.lang.String sha256, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function1<? super java.lang.String, kotlin.Unit> on_log, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function1<? super java.lang.Integer, kotlin.Unit> on_progress, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.lang.Boolean> $completion) {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public static final java.lang.Object install_cmake_from_archive(@org.jetbrains.annotations.NotNull()
    java.lang.String archive_path, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function1<? super java.lang.String, kotlin.Unit> on_log, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function1<? super java.lang.Integer, kotlin.Unit> on_progress, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.lang.Boolean> $completion) {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public static final java.lang.Object uninstall_cmake_tool(@org.jetbrains.annotations.NotNull()
    java.lang.String version, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function1<? super java.lang.String, kotlin.Unit> on_log, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function1<? super java.lang.Integer, kotlin.Unit> on_progress, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.lang.Boolean> $completion) {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public static final java.lang.Object install_ndk_from_url(@org.jetbrains.annotations.NotNull()
    java.lang.String version, @org.jetbrains.annotations.NotNull()
    java.lang.String url, @org.jetbrains.annotations.NotNull()
    java.lang.String sha256, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function1<? super java.lang.String, kotlin.Unit> on_log, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function1<? super java.lang.Integer, kotlin.Unit> on_progress, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.lang.Boolean> $completion) {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public static final java.lang.Object install_ndk_from_archive(@org.jetbrains.annotations.NotNull()
    java.lang.String archive_path, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function1<? super java.lang.String, kotlin.Unit> on_log, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function1<? super java.lang.Integer, kotlin.Unit> on_progress, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.lang.Boolean> $completion) {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public static final java.lang.Object uninstall_ndk_tool(@org.jetbrains.annotations.NotNull()
    java.lang.String version, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function1<? super java.lang.String, kotlin.Unit> on_log, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function1<? super java.lang.Integer, kotlin.Unit> on_progress, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.lang.Boolean> $completion) {
        return null;
    }
    
    private static final java.lang.String create_cmake_install_command(java.lang.String archive_path, java.lang.String download_url, java.lang.String sha256) {
        return null;
    }
    
    private static final java.lang.Object run_cmake_install_task(java.lang.String command, kotlin.jvm.functions.Function1<? super java.lang.String, kotlin.Unit> on_log, kotlin.jvm.functions.Function1<? super java.lang.Integer, kotlin.Unit> on_progress, kotlin.coroutines.Continuation<? super java.lang.Boolean> $completion) {
        return null;
    }
    
    private static final java.lang.Object install_bundled_ninja(java.lang.String cmake_target, kotlin.jvm.functions.Function1<? super java.lang.String, kotlin.Unit> on_log, kotlin.coroutines.Continuation<? super java.lang.Boolean> $completion) {
        return null;
    }
    
    private static final java.lang.Object run_proot_toolchain_task(java.lang.String start_log, java.lang.String command, java.lang.String success_log, kotlin.jvm.functions.Function1<? super java.lang.String, kotlin.Unit> on_log, kotlin.jvm.functions.Function1<? super java.lang.Integer, kotlin.Unit> on_progress, kotlin.coroutines.Continuation<? super java.lang.Boolean> $completion) {
        return null;
    }
    
    private static final java.lang.Object run_ndk_install_task(java.lang.String command, kotlin.jvm.functions.Function1<? super java.lang.String, kotlin.Unit> on_log, kotlin.jvm.functions.Function1<? super java.lang.Integer, kotlin.Unit> on_progress, kotlin.coroutines.Continuation<? super java.lang.Boolean> $completion) {
        return null;
    }
    
    private static final java.lang.String create_download_block(java.lang.String url, java.lang.String sha256) {
        return null;
    }
    
    private static final java.lang.String create_ndk_install_command(java.lang.String archive_path, java.lang.String target_name, java.lang.String download_url, java.lang.String sha256) {
        return null;
    }
    
    private static final void update_ndk_progress_from_log(java.lang.String line, kotlin.jvm.functions.Function1<? super java.lang.Integer, kotlin.Unit> on_progress) {
    }
    
    private static final java.lang.String host_path_to_proot_path(java.lang.String path) {
        return null;
    }
    
    private static final java.lang.String archive_name_from_url(java.lang.String url) {
        return null;
    }
    
    private static final java.lang.String strip_archive_extension(java.lang.String name) {
        return null;
    }
    
    private static final java.lang.String normalize_cmake_install_name(java.lang.String value) {
        return null;
    }
    
    private static final java.lang.String normalize_ndk_install_name(java.lang.String value) {
        return null;
    }
    
    private static final java.lang.String normalize_version_key(java.lang.String value) {
        return null;
    }
    
    private static final java.lang.String shell_quote(java.lang.String value) {
        return null;
    }
    
    private static final java.lang.String with_shell_dollar(java.lang.String $this$with_shell_dollar) {
        return null;
    }
    
    private static final java.lang.Object emit_log(kotlin.jvm.functions.Function1<? super java.lang.String, kotlin.Unit> on_log, java.lang.String message, kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
    
    private static final java.lang.Object emit_progress(kotlin.jvm.functions.Function1<? super java.lang.Integer, kotlin.Unit> on_progress, int progress, kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
}