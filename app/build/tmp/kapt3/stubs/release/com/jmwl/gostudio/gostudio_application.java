package com.jmwl.gostudio;

@kotlin.Metadata(mv = {2, 3, 0}, k = 1, xi = 48, d1 = {"\u0000:\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\u000b\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0010\u0002\n\u0002\b\u0005\n\u0002\u0010\u000e\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\u0003\n\u0002\b\u0004\u0018\u0000 \u001d2\u00020\u0001:\u0001\u001dB\u0007\u00a2\u0006\u0004\b\u0002\u0010\u0003J\b\u0010\f\u001a\u00020\rH\u0016J\b\u0010\u000e\u001a\u00020\rH\u0002J\b\u0010\u000f\u001a\u00020\rH\u0002J\u000e\u0010\u0010\u001a\u00020\r2\u0006\u0010\u0011\u001a\u00020\u0005J\u000e\u0010\u0012\u001a\u00020\u00132\u0006\u0010\u0014\u001a\u00020\u0013J\u0010\u0010\u0015\u001a\u0004\u0018\u00010\u00162\u0006\u0010\u0014\u001a\u00020\u0013J\b\u0010\u0017\u001a\u00020\rH\u0002J\u0010\u0010\u0018\u001a\u00020\r2\u0006\u0010\u0019\u001a\u00020\u001aH\u0002J\u0010\u0010\u001b\u001a\u00020\u00132\u0006\u0010\u0019\u001a\u00020\u001aH\u0002J\b\u0010\u001c\u001a\u00020\rH\u0002R\u000e\u0010\u0004\u001a\u00020\u0005X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u001c\u0010\u0006\u001a\u0004\u0018\u00010\u0007X\u0086\u000e\u00a2\u0006\u000e\n\u0000\u001a\u0004\b\b\u0010\t\"\u0004\b\n\u0010\u000b\u00a8\u0006\u001e"}, d2 = {"Lcom/jmwl/gostudio/gostudio_application;", "Landroid/app/Application;", "<init>", "()V", "textmate_initialized", "", "keep_alive_service_", "Lcom/jmwl/gostudio/service/keep_alive_service;", "getKeep_alive_service_", "()Lcom/jmwl/gostudio/service/keep_alive_service;", "setKeep_alive_service_", "(Lcom/jmwl/gostudio/service/keep_alive_service;)V", "onCreate", "", "init_textmate", "configure_textmate_regex_engine", "set_textmate_theme", "is_dark", "get_language_scope_name", "", "file_name", "create_textmate_language", "Lio/github/rosemoe/sora/langs/textmate/TextMateLanguage;", "setup_uncaught_exception_handler", "handle_crash", "throwable", "", "get_stack_trace_string", "start_keep_alive_service", "Companion", "app_release"})
public final class gostudio_application extends android.app.Application {
    public static com.jmwl.gostudio.gostudio_application instance;
    private boolean textmate_initialized = false;
    @org.jetbrains.annotations.Nullable()
    private com.jmwl.gostudio.service.keep_alive_service keep_alive_service_;
    @org.jetbrains.annotations.NotNull()
    public static final com.jmwl.gostudio.gostudio_application.Companion Companion = null;
    
    public gostudio_application() {
        super();
    }
    
    @org.jetbrains.annotations.Nullable()
    public final com.jmwl.gostudio.service.keep_alive_service getKeep_alive_service_() {
        return null;
    }
    
    public final void setKeep_alive_service_(@org.jetbrains.annotations.Nullable()
    com.jmwl.gostudio.service.keep_alive_service p0) {
    }
    
    @java.lang.Override()
    public void onCreate() {
    }
    
    private final void init_textmate() {
    }
    
    private final void configure_textmate_regex_engine() {
    }
    
    public final void set_textmate_theme(boolean is_dark) {
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String get_language_scope_name(@org.jetbrains.annotations.NotNull()
    java.lang.String file_name) {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final io.github.rosemoe.sora.langs.textmate.TextMateLanguage create_textmate_language(@org.jetbrains.annotations.NotNull()
    java.lang.String file_name) {
        return null;
    }
    
    private final void setup_uncaught_exception_handler() {
    }
    
    private final void handle_crash(java.lang.Throwable throwable) {
    }
    
    private final java.lang.String get_stack_trace_string(java.lang.Throwable throwable) {
        return null;
    }
    
    private final void start_keep_alive_service() {
    }
    
    @kotlin.Metadata(mv = {2, 3, 0}, k = 1, xi = 48, d1 = {"\u0000\u0014\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0005\b\u0086\u0003\u0018\u00002\u00020\u0001B\t\b\u0002\u00a2\u0006\u0004\b\u0002\u0010\u0003R\u001a\u0010\u0004\u001a\u00020\u0005X\u0086.\u00a2\u0006\u000e\n\u0000\u001a\u0004\b\u0006\u0010\u0007\"\u0004\b\b\u0010\t\u00a8\u0006\n"}, d2 = {"Lcom/jmwl/gostudio/gostudio_application$Companion;", "", "<init>", "()V", "instance", "Lcom/jmwl/gostudio/gostudio_application;", "getInstance", "()Lcom/jmwl/gostudio/gostudio_application;", "setInstance", "(Lcom/jmwl/gostudio/gostudio_application;)V", "app_release"})
    public static final class Companion {
        
        private Companion() {
            super();
        }
        
        @org.jetbrains.annotations.NotNull()
        public final com.jmwl.gostudio.gostudio_application getInstance() {
            return null;
        }
        
        public final void setInstance(@org.jetbrains.annotations.NotNull()
        com.jmwl.gostudio.gostudio_application p0) {
        }
    }
}