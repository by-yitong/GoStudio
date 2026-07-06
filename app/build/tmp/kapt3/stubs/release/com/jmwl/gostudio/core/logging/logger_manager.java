package com.jmwl.gostudio.core.logging;

@kotlin.Metadata(mv = {2, 3, 0}, k = 1, xi = 48, d1 = {"\u0000F\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0003\n\u0002\u0010\u000e\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u000b\n\u0002\b\n\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0007\n\u0002\u0010\u0003\n\u0002\b\u0003\b\u00c6\u0002\u0018\u00002\u00020\u0001B\t\b\u0002\u00a2\u0006\u0004\b\u0002\u0010\u0003J\u000e\u0010\u0019\u001a\u00020\u001a2\u0006\u0010\u001b\u001a\u00020\u001cJ\u0016\u0010\u001d\u001a\u00020\u001a2\u0006\u0010\u001e\u001a\u00020\u00052\u0006\u0010\u001f\u001a\u00020\u0005J\u0016\u0010 \u001a\u00020\u001a2\u0006\u0010\u001e\u001a\u00020\u00052\u0006\u0010\u001f\u001a\u00020\u0005J\u0016\u0010!\u001a\u00020\u001a2\u0006\u0010\u001e\u001a\u00020\u00052\u0006\u0010\u001f\u001a\u00020\u0005J\u0016\u0010\"\u001a\u00020\u001a2\u0006\u0010\u001e\u001a\u00020\u00052\u0006\u0010\u001f\u001a\u00020\u0005J \u0010!\u001a\u00020\u001a2\u0006\u0010\u001e\u001a\u00020\u00052\u0006\u0010\u001f\u001a\u00020\u00052\b\u0010#\u001a\u0004\u0018\u00010$J\u0010\u0010%\u001a\u00020\u001a2\u0006\u0010&\u001a\u00020\u0005H\u0002R\u000e\u0010\u0004\u001a\u00020\u0005X\u0082T\u00a2\u0006\u0002\n\u0000R\u0010\u0010\u0006\u001a\u0004\u0018\u00010\u0007X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u0016\u0010\b\u001a\n \n*\u0004\u0018\u00010\t0\tX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u000b\u001a\u00020\fX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\r\u001a\u00020\fX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u001a\u0010\u000e\u001a\u00020\u000fX\u0086\u000e\u00a2\u0006\u000e\n\u0000\u001a\u0004\b\u000e\u0010\u0010\"\u0004\b\u0011\u0010\u0012R\u001a\u0010\u0013\u001a\u00020\u000fX\u0086\u000e\u00a2\u0006\u000e\n\u0000\u001a\u0004\b\u0014\u0010\u0010\"\u0004\b\u0015\u0010\u0012R\u001a\u0010\u0016\u001a\u00020\u000fX\u0086\u000e\u00a2\u0006\u000e\n\u0000\u001a\u0004\b\u0017\u0010\u0010\"\u0004\b\u0018\u0010\u0012\u00a8\u0006\'"}, d2 = {"Lcom/jmwl/gostudio/core/logging/logger_manager;", "", "<init>", "()V", "TAG", "", "log_dir", "Ljava/io/File;", "executor", "Ljava/util/concurrent/ExecutorService;", "kotlin.jvm.PlatformType", "file_date_format", "Ljava/text/SimpleDateFormat;", "date_format", "is_debug", "", "()Z", "set_debug", "(Z)V", "enable_file_log", "getEnable_file_log", "setEnable_file_log", "errors_only", "getErrors_only", "setErrors_only", "init", "", "context", "Landroid/content/Context;", "d", "tag", "msg", "i", "e", "w", "tr", "", "write_to_file", "log", "app_release"})
public final class logger_manager {
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String TAG = "XCode";
    @org.jetbrains.annotations.Nullable()
    private static java.io.File log_dir;
    private static final java.util.concurrent.ExecutorService executor = null;
    @org.jetbrains.annotations.NotNull()
    private static final java.text.SimpleDateFormat file_date_format = null;
    @org.jetbrains.annotations.NotNull()
    private static final java.text.SimpleDateFormat date_format = null;
    private static boolean is_debug = false;
    private static boolean enable_file_log = true;
    private static boolean errors_only = true;
    @org.jetbrains.annotations.NotNull()
    public static final com.jmwl.gostudio.core.logging.logger_manager INSTANCE = null;
    
    private logger_manager() {
        super();
    }
    
    public final boolean is_debug() {
        return false;
    }
    
    public final void set_debug(boolean p0) {
    }
    
    public final boolean getEnable_file_log() {
        return false;
    }
    
    public final void setEnable_file_log(boolean p0) {
    }
    
    public final boolean getErrors_only() {
        return false;
    }
    
    public final void setErrors_only(boolean p0) {
    }
    
    public final void init(@org.jetbrains.annotations.NotNull()
    android.content.Context context) {
    }
    
    public final void d(@org.jetbrains.annotations.NotNull()
    java.lang.String tag, @org.jetbrains.annotations.NotNull()
    java.lang.String msg) {
    }
    
    public final void i(@org.jetbrains.annotations.NotNull()
    java.lang.String tag, @org.jetbrains.annotations.NotNull()
    java.lang.String msg) {
    }
    
    public final void e(@org.jetbrains.annotations.NotNull()
    java.lang.String tag, @org.jetbrains.annotations.NotNull()
    java.lang.String msg) {
    }
    
    public final void w(@org.jetbrains.annotations.NotNull()
    java.lang.String tag, @org.jetbrains.annotations.NotNull()
    java.lang.String msg) {
    }
    
    public final void e(@org.jetbrains.annotations.NotNull()
    java.lang.String tag, @org.jetbrains.annotations.NotNull()
    java.lang.String msg, @org.jetbrains.annotations.Nullable()
    java.lang.Throwable tr) {
    }
    
    private final void write_to_file(java.lang.String log) {
    }
}