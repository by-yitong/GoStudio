package com.jmwl.gostudio.ui.theme;

@kotlin.Metadata(mv = {2, 3, 0}, k = 1, xi = 48, d1 = {"\u0000<\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\u0007\n\u0002\b\u0003\n\u0002\u0010\u000e\n\u0002\b\u0003\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0005\b\u00c6\u0002\u0018\u00002\u00020\u0001B\t\b\u0002\u00a2\u0006\u0004\b\u0002\u0010\u0003J\u000e\u0010\u0013\u001a\u00020\u00142\u0006\u0010\u0015\u001a\u00020\u0016J\u0016\u0010\u0017\u001a\u00020\u00142\u0006\u0010\u0015\u001a\u00020\u00162\u0006\u0010\u0018\u001a\u00020\u0006J\u000e\u0010\u0019\u001a\u00020\f2\u0006\u0010\u0015\u001a\u00020\u0016J\u0016\u0010\u001a\u001a\u00020\u00142\u0006\u0010\u0015\u001a\u00020\u00162\u0006\u0010\r\u001a\u00020\fR\u0014\u0010\u0004\u001a\b\u0012\u0004\u0012\u00020\u00060\u0005X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0017\u0010\u0007\u001a\b\u0012\u0004\u0012\u00020\u00060\b\u00a2\u0006\b\n\u0000\u001a\u0004\b\t\u0010\nR\u0014\u0010\u000b\u001a\b\u0012\u0004\u0012\u00020\f0\u0005X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0017\u0010\r\u001a\b\u0012\u0004\u0012\u00020\f0\b\u00a2\u0006\b\n\u0000\u001a\u0004\b\u000e\u0010\nR\u000e\u0010\u000f\u001a\u00020\u0010X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0011\u001a\u00020\u0010X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0012\u001a\u00020\u0010X\u0082T\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u001b"}, d2 = {"Lcom/jmwl/gostudio/ui/theme/theme_manager;", "", "<init>", "()V", "_theme", "Lkotlinx/coroutines/flow/MutableStateFlow;", "Lcom/jmwl/gostudio/ui/theme/app_theme_type;", "theme", "Lkotlinx/coroutines/flow/StateFlow;", "getTheme", "()Lkotlinx/coroutines/flow/StateFlow;", "_scale", "", "scale", "getScale", "THEME_KEY", "", "SCALE_KEY", "PREFS", "init", "", "context", "Landroid/content/Context;", "set_theme", "type", "get_scale", "set_scale", "app_debug"})
public final class theme_manager {
    @org.jetbrains.annotations.NotNull()
    private static final kotlinx.coroutines.flow.MutableStateFlow<com.jmwl.gostudio.ui.theme.app_theme_type> _theme = null;
    @org.jetbrains.annotations.NotNull()
    private static final kotlinx.coroutines.flow.StateFlow<com.jmwl.gostudio.ui.theme.app_theme_type> theme = null;
    @org.jetbrains.annotations.NotNull()
    private static final kotlinx.coroutines.flow.MutableStateFlow<java.lang.Float> _scale = null;
    @org.jetbrains.annotations.NotNull()
    private static final kotlinx.coroutines.flow.StateFlow<java.lang.Float> scale = null;
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String THEME_KEY = "theme_type";
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String SCALE_KEY = "app_scale";
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String PREFS = "app_settings";
    @org.jetbrains.annotations.NotNull()
    public static final com.jmwl.gostudio.ui.theme.theme_manager INSTANCE = null;
    
    private theme_manager() {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.StateFlow<com.jmwl.gostudio.ui.theme.app_theme_type> getTheme() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.StateFlow<java.lang.Float> getScale() {
        return null;
    }
    
    public final void init(@org.jetbrains.annotations.NotNull()
    android.content.Context context) {
    }
    
    public final void set_theme(@org.jetbrains.annotations.NotNull()
    android.content.Context context, @org.jetbrains.annotations.NotNull()
    com.jmwl.gostudio.ui.theme.app_theme_type type) {
    }
    
    public final float get_scale(@org.jetbrains.annotations.NotNull()
    android.content.Context context) {
        return 0.0F;
    }
    
    public final void set_scale(@org.jetbrains.annotations.NotNull()
    android.content.Context context, float scale) {
    }
}