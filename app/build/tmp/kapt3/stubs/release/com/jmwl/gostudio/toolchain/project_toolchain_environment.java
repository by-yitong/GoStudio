package com.jmwl.gostudio.toolchain;

/**
 * 项目构建环境（Go 版本）。
 *
 * @param environment proot 内的环境变量（PATH/GOROOT/GOPATH/GOPROXY 等）
 * @param go 已装的 Go 工具链；null 表示未装
 * @param missing 缺失项的人类可读描述
 * @param ndk 兼容字段（永远 null；editor_activity 的 cmake 死代码引用，阶段5清理）
 */
@kotlin.Metadata(mv = {2, 3, 0}, k = 1, xi = 48, d1 = {"\u00008\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010$\n\u0002\u0010\u000e\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010 \n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0010\n\u0002\u0010\u000b\n\u0002\b\u0002\n\u0002\u0010\b\n\u0002\b\u0002\b\u0086\b\u0018\u00002\u00020\u0001B?\u0012\u0012\u0010\u0002\u001a\u000e\u0012\u0004\u0012\u00020\u0004\u0012\u0004\u0012\u00020\u00040\u0003\u0012\b\u0010\u0005\u001a\u0004\u0018\u00010\u0006\u0012\f\u0010\u0007\u001a\b\u0012\u0004\u0012\u00020\u00040\b\u0012\n\b\u0002\u0010\t\u001a\u0004\u0018\u00010\n\u00a2\u0006\u0004\b\u000b\u0010\fJ\u0015\u0010\u0015\u001a\u000e\u0012\u0004\u0012\u00020\u0004\u0012\u0004\u0012\u00020\u00040\u0003H\u00c6\u0003J\u000b\u0010\u0016\u001a\u0004\u0018\u00010\u0006H\u00c6\u0003J\u000f\u0010\u0017\u001a\b\u0012\u0004\u0012\u00020\u00040\bH\u00c6\u0003J\u000b\u0010\u0018\u001a\u0004\u0018\u00010\nH\u00c6\u0003JG\u0010\u0019\u001a\u00020\u00002\u0014\b\u0002\u0010\u0002\u001a\u000e\u0012\u0004\u0012\u00020\u0004\u0012\u0004\u0012\u00020\u00040\u00032\n\b\u0002\u0010\u0005\u001a\u0004\u0018\u00010\u00062\u000e\b\u0002\u0010\u0007\u001a\b\u0012\u0004\u0012\u00020\u00040\b2\n\b\u0002\u0010\t\u001a\u0004\u0018\u00010\nH\u00c6\u0001J\u0014\u0010\u001a\u001a\u00020\u001b2\b\u0010\u001c\u001a\u0004\u0018\u00010\u0001H\u00d6\u0083\u0004J\n\u0010\u001d\u001a\u00020\u001eH\u00d6\u0081\u0004J\n\u0010\u001f\u001a\u00020\u0004H\u00d6\u0081\u0004R\u001d\u0010\u0002\u001a\u000e\u0012\u0004\u0012\u00020\u0004\u0012\u0004\u0012\u00020\u00040\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\r\u0010\u000eR\u0013\u0010\u0005\u001a\u0004\u0018\u00010\u0006\u00a2\u0006\b\n\u0000\u001a\u0004\b\u000f\u0010\u0010R\u0017\u0010\u0007\u001a\b\u0012\u0004\u0012\u00020\u00040\b\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0011\u0010\u0012R\u0013\u0010\t\u001a\u0004\u0018\u00010\n\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0013\u0010\u0014\u00a8\u0006 "}, d2 = {"Lcom/jmwl/gostudio/toolchain/project_toolchain_environment;", "", "environment", "", "", "go", "Lcom/jmwl/gostudio/toolchain/go_toolchain_info;", "missing", "", "ndk", "Lcom/jmwl/gostudio/toolchain/ndk_toolchain_info;", "<init>", "(Ljava/util/Map;Lcom/jmwl/gostudio/toolchain/go_toolchain_info;Ljava/util/List;Lcom/jmwl/gostudio/toolchain/ndk_toolchain_info;)V", "getEnvironment", "()Ljava/util/Map;", "getGo", "()Lcom/jmwl/gostudio/toolchain/go_toolchain_info;", "getMissing", "()Ljava/util/List;", "getNdk", "()Lcom/jmwl/gostudio/toolchain/ndk_toolchain_info;", "component1", "component2", "component3", "component4", "copy", "equals", "", "other", "hashCode", "", "toString", "app_release"})
public final class project_toolchain_environment {
    @org.jetbrains.annotations.NotNull()
    private final java.util.Map<java.lang.String, java.lang.String> environment = null;
    @org.jetbrains.annotations.Nullable()
    private final com.jmwl.gostudio.toolchain.go_toolchain_info go = null;
    @org.jetbrains.annotations.NotNull()
    private final java.util.List<java.lang.String> missing = null;
    @org.jetbrains.annotations.Nullable()
    private final com.jmwl.gostudio.toolchain.ndk_toolchain_info ndk = null;
    
    public project_toolchain_environment(@org.jetbrains.annotations.NotNull()
    java.util.Map<java.lang.String, java.lang.String> environment, @org.jetbrains.annotations.Nullable()
    com.jmwl.gostudio.toolchain.go_toolchain_info go, @org.jetbrains.annotations.NotNull()
    java.util.List<java.lang.String> missing, @org.jetbrains.annotations.Nullable()
    com.jmwl.gostudio.toolchain.ndk_toolchain_info ndk) {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.util.Map<java.lang.String, java.lang.String> getEnvironment() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final com.jmwl.gostudio.toolchain.go_toolchain_info getGo() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.util.List<java.lang.String> getMissing() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final com.jmwl.gostudio.toolchain.ndk_toolchain_info getNdk() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.util.Map<java.lang.String, java.lang.String> component1() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final com.jmwl.gostudio.toolchain.go_toolchain_info component2() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.util.List<java.lang.String> component3() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final com.jmwl.gostudio.toolchain.ndk_toolchain_info component4() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.jmwl.gostudio.toolchain.project_toolchain_environment copy(@org.jetbrains.annotations.NotNull()
    java.util.Map<java.lang.String, java.lang.String> environment, @org.jetbrains.annotations.Nullable()
    com.jmwl.gostudio.toolchain.go_toolchain_info go, @org.jetbrains.annotations.NotNull()
    java.util.List<java.lang.String> missing, @org.jetbrains.annotations.Nullable()
    com.jmwl.gostudio.toolchain.ndk_toolchain_info ndk) {
        return null;
    }
    
    @java.lang.Override()
    public boolean equals(@org.jetbrains.annotations.Nullable()
    java.lang.Object other) {
        return false;
    }
    
    @java.lang.Override()
    public int hashCode() {
        return 0;
    }
    
    @java.lang.Override()
    @org.jetbrains.annotations.NotNull()
    public java.lang.String toString() {
        return null;
    }
}