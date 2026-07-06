package com.jmwl.gostudio.editor.settings;

@kotlin.Metadata(mv = {2, 3, 0}, k = 2, xi = 48, d1 = {"\u0000>\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u000b\n\u0000\n\u0002\u0010\b\n\u0002\b\u0004\n\u0002\u0010\"\n\u0002\b\u0003\u001a4\u0010\u0000\u001a\u00020\u00012\u0006\u0010\u0002\u001a\u00020\u00032\u0006\u0010\u0004\u001a\u00020\u00052\u0006\u0010\u0006\u001a\u00020\u00072\b\u0010\b\u001a\u0004\u0018\u00010\t2\b\u0010\n\u001a\u0004\u0018\u00010\u000bH\u0000\u001a\u001a\u0010\f\u001a\u00020\u00012\u0006\u0010\u0004\u001a\u00020\u00052\b\u0010\b\u001a\u0004\u0018\u00010\tH\u0002\u001a \u0010\r\u001a\u00020\u000e2\u0006\u0010\u0004\u001a\u00020\u00052\u0006\u0010\u000f\u001a\u00020\u00102\u0006\u0010\u0011\u001a\u00020\u0010H\u0002\u001a \u0010\u0012\u001a\u00020\u000e2\u0006\u0010\u0004\u001a\u00020\u00052\u0006\u0010\u000f\u001a\u00020\t2\u0006\u0010\u0013\u001a\u00020\u0010H\u0002\u001a\"\u0010\u0016\u001a\u00020\u00012\u0006\u0010\u0017\u001a\u00020\u000b2\u0006\u0010\u0006\u001a\u00020\u00072\b\u0010\b\u001a\u0004\u0018\u00010\tH\u0000\"\u0014\u0010\u0014\u001a\b\u0012\u0004\u0012\u00020\t0\u0015X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0018"}, d2 = {"apply_editor_behavior_settings", "", "context", "Landroid/content/Context;", "editor", "Lio/github/rosemoe/sora/widget/CodeEditor;", "settings", "Lcom/jmwl/gostudio/editor/model/editor_settings_state;", "file_path", "", "current_language", "Lio/github/rosemoe/sora/langs/textmate/TextMateLanguage;", "apply_editor_symbol_pairs", "is_textmate_non_code_token", "", "line", "", "column", "should_complete_angle_pair", "left_column", "angle_pair_context_names", "", "apply_textmate_language_settings", "language", "app_debug"})
public final class Editor_behavior_helperKt {
    @org.jetbrains.annotations.NotNull()
    private static final java.util.Set<java.lang.String> angle_pair_context_names = null;
    
    public static final void apply_editor_behavior_settings(@org.jetbrains.annotations.NotNull()
    android.content.Context context, @org.jetbrains.annotations.NotNull()
    io.github.rosemoe.sora.widget.CodeEditor editor, @org.jetbrains.annotations.NotNull()
    com.jmwl.gostudio.editor.model.editor_settings_state settings, @org.jetbrains.annotations.Nullable()
    java.lang.String file_path, @org.jetbrains.annotations.Nullable()
    io.github.rosemoe.sora.langs.textmate.TextMateLanguage current_language) {
    }
    
    private static final void apply_editor_symbol_pairs(io.github.rosemoe.sora.widget.CodeEditor editor, java.lang.String file_path) {
    }
    
    private static final boolean is_textmate_non_code_token(io.github.rosemoe.sora.widget.CodeEditor editor, int line, int column) {
        return false;
    }
    
    private static final boolean should_complete_angle_pair(io.github.rosemoe.sora.widget.CodeEditor editor, java.lang.String line, int left_column) {
        return false;
    }
    
    public static final void apply_textmate_language_settings(@org.jetbrains.annotations.NotNull()
    io.github.rosemoe.sora.langs.textmate.TextMateLanguage language, @org.jetbrains.annotations.NotNull()
    com.jmwl.gostudio.editor.model.editor_settings_state settings, @org.jetbrains.annotations.Nullable()
    java.lang.String file_path) {
    }
}