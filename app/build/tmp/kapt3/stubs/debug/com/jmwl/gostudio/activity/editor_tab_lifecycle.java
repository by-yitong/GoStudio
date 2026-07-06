package com.jmwl.gostudio.activity;

@kotlin.Metadata(mv = {2, 3, 0}, k = 1, xi = 48, d1 = {"\u0000V\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\u0010\u000e\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000b\n\u0002\b\u0002\n\u0002\u0010\t\n\u0002\b\u0006\n\u0002\u0018\u0002\n\u0002\b\u0006\n\u0002\u0010\r\n\u0002\b\u0003\b\u0000\u0018\u00002\u00020\u0001B\u0093\u0001\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\f\u0010\u0004\u001a\b\u0012\u0004\u0012\u00020\u00060\u0005\u0012\u0012\u0010\u0007\u001a\u000e\u0012\u0004\u0012\u00020\t\u0012\u0004\u0012\u00020\n0\b\u0012\u0018\u0010\u000b\u001a\u0014\u0012\n\u0012\b\u0012\u0004\u0012\u00020\f0\u0005\u0012\u0004\u0012\u00020\f0\b\u0012\f\u0010\r\u001a\b\u0012\u0004\u0012\u00020\f0\u0005\u0012\u0012\u0010\u000e\u001a\u000e\u0012\u0004\u0012\u00020\u000f\u0012\u0004\u0012\u00020\f0\b\u0012\u000e\u0010\u0010\u001a\n\u0012\u0006\u0012\u0004\u0018\u00010\u00110\u0005\u0012\f\u0010\u0012\u001a\b\u0012\u0004\u0012\u00020\f0\u0005\u0012\u0006\u0010\u0013\u001a\u00020\u0014\u00a2\u0006\u0004\b\u0015\u0010\u0016J\u0010\u0010\u0017\u001a\u00020\u000f2\b\u0010\u0018\u001a\u0004\u0018\u00010\tJ\u0016\u0010\u0019\u001a\u00020\u000f2\u0006\u0010\u001a\u001a\u00020\u001bH\u0086@\u00a2\u0006\u0002\u0010\u001cJ\u000e\u0010\u001d\u001a\u00020\u000f2\u0006\u0010\u001a\u001a\u00020\u001bJ\u000e\u0010\u001e\u001a\u00020\f2\u0006\u0010\u001a\u001a\u00020\u001bJ\u0016\u0010\u001f\u001a\u00020\f2\u0006\u0010 \u001a\u00020\u000f2\u0006\u0010!\u001a\u00020\"J\u0016\u0010#\u001a\u00020\f2\u0006\u0010 \u001a\u00020\u000fH\u0082@\u00a2\u0006\u0002\u0010$R\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0014\u0010\u0004\u001a\b\u0012\u0004\u0012\u00020\u00060\u0005X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u001a\u0010\u0007\u001a\u000e\u0012\u0004\u0012\u00020\t\u0012\u0004\u0012\u00020\n0\bX\u0082\u0004\u00a2\u0006\u0002\n\u0000R \u0010\u000b\u001a\u0014\u0012\n\u0012\b\u0012\u0004\u0012\u00020\f0\u0005\u0012\u0004\u0012\u00020\f0\bX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0014\u0010\r\u001a\b\u0012\u0004\u0012\u00020\f0\u0005X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u001a\u0010\u000e\u001a\u000e\u0012\u0004\u0012\u00020\u000f\u0012\u0004\u0012\u00020\f0\bX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0016\u0010\u0010\u001a\n\u0012\u0006\u0012\u0004\u0018\u00010\u00110\u0005X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0014\u0010\u0012\u001a\b\u0012\u0004\u0012\u00020\f0\u0005X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0013\u001a\u00020\u0014X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006%"}, d2 = {"Lcom/jmwl/gostudio/activity/editor_tab_lifecycle;", "", "context", "Landroid/content/Context;", "settings", "Lkotlin/Function0;", "Lcom/jmwl/gostudio/editor/model/editor_settings_state;", "create_textmate_language", "Lkotlin/Function1;", "", "Lio/github/rosemoe/sora/langs/textmate/TextMateLanguage;", "with_applying_content", "", "on_content_changed", "on_selection_changed", "Lio/github/rosemoe/sora/widget/CodeEditor;", "current_comment_action", "", "on_toggle_comment", "initial_styles_timeout_ms", "", "<init>", "(Landroid/content/Context;Lkotlin/jvm/functions/Function0;Lkotlin/jvm/functions/Function1;Lkotlin/jvm/functions/Function1;Lkotlin/jvm/functions/Function0;Lkotlin/jvm/functions/Function1;Lkotlin/jvm/functions/Function0;Lkotlin/jvm/functions/Function0;J)V", "create_editor", "file_path", "prepare_for_display", "tab", "Lcom/jmwl/gostudio/editor/session/editor_open_tab;", "(Lcom/jmwl/gostudio/editor/session/editor_open_tab;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "prepare", "release", "set_content", "target", "content", "", "wait_for_initial_styles", "(Lio/github/rosemoe/sora/widget/CodeEditor;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "app_debug"})
public final class editor_tab_lifecycle {
    @org.jetbrains.annotations.NotNull()
    private final android.content.Context context = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlin.jvm.functions.Function0<com.jmwl.gostudio.editor.model.editor_settings_state> settings = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlin.jvm.functions.Function1<java.lang.String, io.github.rosemoe.sora.langs.textmate.TextMateLanguage> create_textmate_language = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlin.jvm.functions.Function1<kotlin.jvm.functions.Function0<kotlin.Unit>, kotlin.Unit> with_applying_content = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlin.jvm.functions.Function0<kotlin.Unit> on_content_changed = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlin.jvm.functions.Function1<io.github.rosemoe.sora.widget.CodeEditor, kotlin.Unit> on_selection_changed = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlin.jvm.functions.Function0<java.lang.Boolean> current_comment_action = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlin.jvm.functions.Function0<kotlin.Unit> on_toggle_comment = null;
    private final long initial_styles_timeout_ms = 0L;
    
    public editor_tab_lifecycle(@org.jetbrains.annotations.NotNull()
    android.content.Context context, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function0<com.jmwl.gostudio.editor.model.editor_settings_state> settings, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function1<? super java.lang.String, ? extends io.github.rosemoe.sora.langs.textmate.TextMateLanguage> create_textmate_language, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function1<? super kotlin.jvm.functions.Function0<kotlin.Unit>, kotlin.Unit> with_applying_content, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function0<kotlin.Unit> on_content_changed, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function1<? super io.github.rosemoe.sora.widget.CodeEditor, kotlin.Unit> on_selection_changed, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function0<java.lang.Boolean> current_comment_action, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function0<kotlin.Unit> on_toggle_comment, long initial_styles_timeout_ms) {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final io.github.rosemoe.sora.widget.CodeEditor create_editor(@org.jetbrains.annotations.Nullable()
    java.lang.String file_path) {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object prepare_for_display(@org.jetbrains.annotations.NotNull()
    com.jmwl.gostudio.editor.session.editor_open_tab tab, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super io.github.rosemoe.sora.widget.CodeEditor> $completion) {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final io.github.rosemoe.sora.widget.CodeEditor prepare(@org.jetbrains.annotations.NotNull()
    com.jmwl.gostudio.editor.session.editor_open_tab tab) {
        return null;
    }
    
    public final void release(@org.jetbrains.annotations.NotNull()
    com.jmwl.gostudio.editor.session.editor_open_tab tab) {
    }
    
    public final void set_content(@org.jetbrains.annotations.NotNull()
    io.github.rosemoe.sora.widget.CodeEditor target, @org.jetbrains.annotations.NotNull()
    java.lang.CharSequence content) {
    }
    
    private final java.lang.Object wait_for_initial_styles(io.github.rosemoe.sora.widget.CodeEditor target, kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
}