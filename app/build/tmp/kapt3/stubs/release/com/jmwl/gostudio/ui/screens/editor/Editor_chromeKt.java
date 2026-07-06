package com.jmwl.gostudio.ui.screens.editor;

@kotlin.Metadata(mv = {2, 3, 0}, k = 2, xi = 48, d1 = {"\u0000:\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0000\n\u0002\u0010\u000b\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0013\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\u001a\u0094\u0001\u0010\u0000\u001a\u00020\u00012\f\u0010\u0002\u001a\b\u0012\u0004\u0012\u00020\u00040\u00032\b\u0010\u0005\u001a\u0004\u0018\u00010\u00062\u0006\u0010\u0007\u001a\u00020\b2\f\u0010\t\u001a\b\u0012\u0004\u0012\u00020\u00010\n2\u0012\u0010\u000b\u001a\u000e\u0012\u0004\u0012\u00020\u0006\u0012\u0004\u0012\u00020\u00010\f2\u0012\u0010\r\u001a\u000e\u0012\u0004\u0012\u00020\u0006\u0012\u0004\u0012\u00020\u00010\f2\u0012\u0010\u000e\u001a\u000e\u0012\u0004\u0012\u00020\u0006\u0012\u0004\u0012\u00020\u00010\f2\u0012\u0010\u000f\u001a\u000e\u0012\u0004\u0012\u00020\u0006\u0012\u0004\u0012\u00020\u00010\f2\f\u0010\u0010\u001a\b\u0012\u0004\u0012\u00020\u00010\nH\u0007\u001a\u0086\u0001\u0010\u0011\u001a\u00020\u00012\u0006\u0010\u0012\u001a\u00020\u00062\u0006\u0010\u0013\u001a\u00020\u00062\u0006\u0010\u0014\u001a\u00020\b2\u0006\u0010\u0015\u001a\u00020\b2\u0006\u0010\u0016\u001a\u00020\b2\f\u0010\u0017\u001a\b\u0012\u0004\u0012\u00020\u00010\n2\f\u0010\u0018\u001a\b\u0012\u0004\u0012\u00020\u00010\n2\f\u0010\u0019\u001a\b\u0012\u0004\u0012\u00020\u00010\n2\f\u0010\u001a\u001a\b\u0012\u0004\u0012\u00020\u00010\n2\u0006\u0010\u001b\u001a\u00020\b2\u0006\u0010\u001c\u001a\u00020\b2\f\u0010\u001d\u001a\b\u0012\u0004\u0012\u00020\u00010\nH\u0007\u001a0\u0010\u001e\u001a\u00020\u00012\u0006\u0010\u001f\u001a\u00020 2\b\b\u0002\u0010!\u001a\u00020\"2\u0014\b\u0002\u0010#\u001a\u000e\u0012\u0004\u0012\u00020\b\u0012\u0004\u0012\u00020\u00010\fH\u0007\u00a8\u0006$"}, d2 = {"editor_tabs_bar", "", "tabs", "", "Lcom/jmwl/gostudio/editor/model/editor_tab_item;", "selected_tab_path", "", "toolbar_visible", "", "on_toggle_toolbar", "Lkotlin/Function0;", "on_select_tab", "Lkotlin/Function1;", "on_pin_tab", "on_close_tab", "on_close_other_tabs", "on_close_all_tabs", "editor_top_bar", "title", "subtitle", "read_only", "has_open_file", "search_visible", "on_toggle_drawer", "on_toggle_search", "on_build", "on_configure_cmake", "build_running", "build_stopping", "on_toggle_read_only", "code_editor_panel", "editor", "Lio/github/rosemoe/sora/widget/CodeEditor;", "modifier", "Landroidx/compose/ui/Modifier;", "on_focus_change", "app_release"})
public final class Editor_chromeKt {
    
    @androidx.compose.runtime.Composable()
    public static final void editor_tabs_bar(@org.jetbrains.annotations.NotNull()
    java.util.List<com.jmwl.gostudio.editor.model.editor_tab_item> tabs, @org.jetbrains.annotations.Nullable()
    java.lang.String selected_tab_path, boolean toolbar_visible, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function0<kotlin.Unit> on_toggle_toolbar, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function1<? super java.lang.String, kotlin.Unit> on_select_tab, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function1<? super java.lang.String, kotlin.Unit> on_pin_tab, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function1<? super java.lang.String, kotlin.Unit> on_close_tab, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function1<? super java.lang.String, kotlin.Unit> on_close_other_tabs, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function0<kotlin.Unit> on_close_all_tabs) {
    }
    
    @kotlin.OptIn(markerClass = {androidx.compose.material3.ExperimentalMaterial3Api.class})
    @androidx.compose.runtime.Composable()
    public static final void editor_top_bar(@org.jetbrains.annotations.NotNull()
    java.lang.String title, @org.jetbrains.annotations.NotNull()
    java.lang.String subtitle, boolean read_only, boolean has_open_file, boolean search_visible, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function0<kotlin.Unit> on_toggle_drawer, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function0<kotlin.Unit> on_toggle_search, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function0<kotlin.Unit> on_build, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function0<kotlin.Unit> on_configure_cmake, boolean build_running, boolean build_stopping, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function0<kotlin.Unit> on_toggle_read_only) {
    }
    
    @androidx.compose.runtime.Composable()
    public static final void code_editor_panel(@org.jetbrains.annotations.NotNull()
    io.github.rosemoe.sora.widget.CodeEditor editor, @org.jetbrains.annotations.NotNull()
    androidx.compose.ui.Modifier modifier, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function1<? super java.lang.Boolean, kotlin.Unit> on_focus_change) {
    }
}