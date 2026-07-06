package com.jmwl.gostudio.ui.screens.editor;

@kotlin.Metadata(mv = {2, 3, 0}, k = 1, xi = 48, d1 = {"\u00008\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\u0010\u000b\n\u0000\n\u0002\u0010\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0010\b\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0006\u0018\u00002\u00020\u0001B-\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u000e\u0010\u0004\u001a\n\u0012\u0006\u0012\u0004\u0018\u00010\u00060\u0005\u0012\f\u0010\u0007\u001a\b\u0012\u0004\u0012\u00020\b0\u0005\u00a2\u0006\u0004\b\t\u0010\nJ\b\u0010\r\u001a\u00020\bH\u0016J\b\u0010\u000e\u001a\u00020\bH\u0014J\b\u0010\u000f\u001a\u00020\fH\u0002J&\u0010\u0010\u001a\u00020\f2\u0006\u0010\u0011\u001a\u00020\u00122\u0006\u0010\u0013\u001a\u00020\u00142\f\u0010\u0015\u001a\b\u0012\u0004\u0012\u00020\b0\u0005H\u0002J\u0010\u0010\u0016\u001a\u00020\b2\u0006\u0010\u0017\u001a\u00020\u0006H\u0002J\b\u0010\u0018\u001a\u00020\bH\u0002J\f\u0010\u0019\u001a\u00020\u0012*\u00020\u0012H\u0002R\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0016\u0010\u0004\u001a\n\u0012\u0006\u0012\u0004\u0018\u00010\u00060\u0005X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0014\u0010\u0007\u001a\b\u0012\u0004\u0012\u00020\b0\u0005X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0010\u0010\u000b\u001a\u0004\u0018\u00010\fX\u0082\u000e\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u001a"}, d2 = {"Lcom/jmwl/gostudio/ui/screens/editor/editor_text_action_window;", "Lio/github/rosemoe/sora/widget/component/EditorTextActionWindow;", "editor", "Lio/github/rosemoe/sora/widget/CodeEditor;", "current_comment_action", "Lkotlin/Function0;", "", "on_toggle_comment", "", "<init>", "(Lio/github/rosemoe/sora/widget/CodeEditor;Lkotlin/jvm/functions/Function0;Lkotlin/jvm/functions/Function0;)V", "comment_button", "Landroid/widget/ImageButton;", "displayWindow", "applyColorScheme", "create_comment_button", "create_action_button", "icon_res", "", "description", "", "on_click", "update_comment_button_mode", "should_uncomment", "apply_action_button_color", "dp_to_px", "app_release"})
public final class editor_text_action_window extends io.github.rosemoe.sora.widget.component.EditorTextActionWindow {
    @org.jetbrains.annotations.NotNull()
    private final io.github.rosemoe.sora.widget.CodeEditor editor = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlin.jvm.functions.Function0<java.lang.Boolean> current_comment_action = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlin.jvm.functions.Function0<kotlin.Unit> on_toggle_comment = null;
    @org.jetbrains.annotations.Nullable()
    private android.widget.ImageButton comment_button;
    
    public editor_text_action_window(@org.jetbrains.annotations.NotNull()
    io.github.rosemoe.sora.widget.CodeEditor editor, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function0<java.lang.Boolean> current_comment_action, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function0<kotlin.Unit> on_toggle_comment) {
        super(null);
    }
    
    @java.lang.Override()
    public void displayWindow() {
    }
    
    @java.lang.Override()
    protected void applyColorScheme() {
    }
    
    private final android.widget.ImageButton create_comment_button() {
        return null;
    }
    
    private final android.widget.ImageButton create_action_button(int icon_res, java.lang.String description, kotlin.jvm.functions.Function0<kotlin.Unit> on_click) {
        return null;
    }
    
    private final void update_comment_button_mode(boolean should_uncomment) {
    }
    
    private final void apply_action_button_color() {
    }
    
    private final int dp_to_px(int $this$dp_to_px) {
        return 0;
    }
}