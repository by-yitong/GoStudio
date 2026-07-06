package com.jmwl.gostudio.toolchain;

@kotlin.Metadata(mv = {2, 3, 0}, k = 1, xi = 48, d1 = {"\u00002\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0006\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\b\u00c6\u0002\u0018\u00002\u00020\u0001B\t\b\u0002\u00a2\u0006\u0004\b\u0002\u0010\u0003J:\u0010\u0006\u001a\u00020\u00072\u0006\u0010\b\u001a\u00020\t2\u0006\u0010\n\u001a\u00020\u000b2\u0006\u0010\f\u001a\u00020\u000b2\u0006\u0010\r\u001a\u00020\u000b2\u0006\u0010\u000e\u001a\u00020\u000b2\n\b\u0002\u0010\u000f\u001a\u0004\u0018\u00010\u000bJ\u0006\u0010\u0010\u001a\u00020\u0005J\u0006\u0010\u0011\u001a\u00020\u0012J\u0006\u0010\u0013\u001a\u00020\u0014R\u000e\u0010\u0004\u001a\u00020\u0005X\u0082.\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0015"}, d2 = {"Lcom/jmwl/gostudio/toolchain/toolchain_runtime_provider;", "", "<init>", "()V", "runtime_paths", "Lcom/jmwl/gostudio/toolchain/runtime/toolchain_runtime_paths;", "init", "", "context", "Landroid/content/Context;", "gostudio_dir", "Ljava/io/File;", "home_dir", "ubuntu_base_dir", "proot_tmp_dir", "external_storage_dir", "paths", "command_builder", "Lcom/jmwl/gostudio/toolchain/runtime/proot_command_builder;", "shell_runner", "Lcom/jmwl/gostudio/toolchain/runtime/proot_shell_runner;", "app_release"})
public final class toolchain_runtime_provider {
    private static com.jmwl.gostudio.toolchain.runtime.toolchain_runtime_paths runtime_paths;
    @org.jetbrains.annotations.NotNull()
    public static final com.jmwl.gostudio.toolchain.toolchain_runtime_provider INSTANCE = null;
    
    private toolchain_runtime_provider() {
        super();
    }
    
    public final void init(@org.jetbrains.annotations.NotNull()
    android.content.Context context, @org.jetbrains.annotations.NotNull()
    java.io.File gostudio_dir, @org.jetbrains.annotations.NotNull()
    java.io.File home_dir, @org.jetbrains.annotations.NotNull()
    java.io.File ubuntu_base_dir, @org.jetbrains.annotations.NotNull()
    java.io.File proot_tmp_dir, @org.jetbrains.annotations.Nullable()
    java.io.File external_storage_dir) {
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.jmwl.gostudio.toolchain.runtime.toolchain_runtime_paths paths() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.jmwl.gostudio.toolchain.runtime.proot_command_builder command_builder() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.jmwl.gostudio.toolchain.runtime.proot_shell_runner shell_runner() {
        return null;
    }
}