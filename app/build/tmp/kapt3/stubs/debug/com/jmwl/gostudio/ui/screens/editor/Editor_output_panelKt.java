package com.jmwl.gostudio.ui.screens.editor;

@kotlin.Metadata(mv = {2, 3, 0}, k = 2, xi = 48, d1 = {"\u0000\u0094\u0001\n\u0000\n\u0002\u0010\b\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0000\n\u0002\u0010$\n\u0000\n\u0002\u0010\u000b\n\u0000\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0010\u0007\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0002\b\n\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\t\u001a\b\u0010\u0004\u001a\u00020\u0005H\u0001\u001a\u0080\u0001\u0010\u0006\u001a\u00020\u00072\u0006\u0010\b\u001a\u00020\u00052\n\u0010\t\u001a\u00060\nj\u0002`\u000b2\u0006\u0010\f\u001a\u00020\r2\u0012\u0010\u000e\u001a\u000e\u0012\u0004\u0012\u00020\r\u0012\u0004\u0012\u00020\r0\u000f2\b\b\u0002\u0010\u0010\u001a\u00020\u00112\u0013\b\u0002\u0010\u0012\u001a\r\u0012\u0004\u0012\u00020\u00070\u0013\u00a2\u0006\u0002\b\u00142\b\b\u0002\u0010\u0015\u001a\u00020\u00162\u001d\u0010\u0017\u001a\u0019\u0012\u0004\u0012\u00020\u0019\u0012\u0004\u0012\u00020\u001a\u0012\u0004\u0012\u00020\u00070\u0018\u00a2\u0006\u0002\b\u0014H\u0001\u001aa\u0010\u001b\u001a\u00020\u00072\u0006\u0010\b\u001a\u00020\u00052\n\u0010\t\u001a\u00060\nj\u0002`\u000b2\u0006\u0010\f\u001a\u00020\r2\u0012\u0010\u000e\u001a\u000e\u0012\u0004\u0012\u00020\r\u0012\u0004\u0012\u00020\r0\u000f2\u0006\u0010\u001c\u001a\u00020\u001a2\u0006\u0010\u001d\u001a\u00020\u001a2\u0006\u0010\u001e\u001a\u00020\u001f2\b\b\u0002\u0010\u0015\u001a\u00020\u0016H\u0003\u00a2\u0006\u0004\b \u0010!\u001ap\u0010\"\u001a\u00020\u00072\u0006\u0010#\u001a\u00020\r2\u0006\u0010$\u001a\u00020\r2\u0006\u0010%\u001a\u00020\u00112\u0006\u0010&\u001a\u00020\u001a2\f\u0010\'\u001a\b\u0012\u0004\u0012\u00020\u00070\u00132\f\u0010(\u001a\b\u0012\u0004\u0012\u00020\u00070\u00132\u0012\u0010)\u001a\u000e\u0012\u0004\u0012\u00020\u001a\u0012\u0004\u0012\u00020\u00070*2\f\u0010+\u001a\b\u0012\u0004\u0012\u00020\u00070\u00132\b\b\u0002\u0010\u0015\u001a\u00020\u0016H\u0003\u001a$\u0010,\u001a\u00020\u00072\u0006\u0010-\u001a\u00020.2\u0012\u0010/\u001a\u000e\u0012\u0004\u0012\u00020.\u0012\u0004\u0012\u00020\u00070*H\u0003\u001aB\u00100\u001a\u00020\u00072\u0006\u0010\b\u001a\u00020\u00052\n\u0010\t\u001a\u00060\nj\u0002`\u000b2\u0006\u0010\f\u001a\u00020\r2\u0012\u0010\u000e\u001a\u000e\u0012\u0004\u0012\u00020\r\u0012\u0004\u0012\u00020\r0\u000f2\b\b\u0002\u0010\u0015\u001a\u00020\u0016H\u0003\u001a(\u00101\u001a\u00020\u00072\f\u00102\u001a\b\u0012\u0004\u0012\u000204032\u0006\u00105\u001a\u00020\u00012\b\b\u0002\u0010\u0015\u001a\u00020\u0016H\u0003\u001a\u001a\u00106\u001a\u00020\u00072\u0006\u00107\u001a\u00020.2\b\b\u0002\u0010\u0015\u001a\u00020\u0016H\u0003\u001a \u00108\u001a\u00020\u00072\u0006\u00109\u001a\u00020:2\u0006\u0010#\u001a\u00020\r2\u0006\u0010;\u001a\u00020\rH\u0002\u001a=\u0010<\u001a\u00020\u00072\u0006\u0010=\u001a\u00020>2\u0006\u0010?\u001a\u00020\r2\u0006\u0010@\u001a\u00020A2\u0006\u0010B\u001a\u00020A2\f\u0010\'\u001a\b\u0012\u0004\u0012\u00020\u00070\u0013H\u0003\u00a2\u0006\u0004\bC\u0010D\u001a+\u0010E\u001a\u00020\u0016*\u00020\u00162\u0006\u0010%\u001a\u00020\u00112\u0006\u0010F\u001a\u00020A2\u0006\u0010G\u001a\u00020\u001aH\u0002\u00a2\u0006\u0004\bH\u0010I\"\u000e\u0010\u0000\u001a\u00020\u0001X\u0082T\u00a2\u0006\u0002\n\u0000\"\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006J"}, d2 = {"editor_output_max_lines", "", "editor_log_time_formatter", "Ljava/time/format/DateTimeFormatter;", "remember_editor_output_panel_state", "Lcom/jmwl/gostudio/ui/screens/editor/editor_output_panel_state;", "editor_output_bottom_sheet_scaffold", "", "state", "terminal_state", "Lcom/jmwl/gostudio/ui/terminal/terminal_state;", "Lcom/jmwl/gostudio/ui/screens/editor/editor_terminal_state;", "terminal_cwd", "", "terminal_extra_environment", "", "show_symbol_bar", "", "symbol_bar", "Lkotlin/Function0;", "Landroidx/compose/runtime/Composable;", "modifier", "Landroidx/compose/ui/Modifier;", "content", "Lkotlin/Function2;", "Landroidx/compose/foundation/layout/PaddingValues;", "", "editor_output_dock_panel", "content_alpha", "button_alpha", "bottom_safe_padding", "Landroidx/compose/ui/unit/Dp;", "editor_output_dock_panel-Hpu9SfQ", "(Lcom/jmwl/gostudio/ui/screens/editor/editor_output_panel_state;Lcom/jmwl/gostudio/ui/terminal/terminal_state;Ljava/lang/String;Ljava/util/Map;FFFLandroidx/compose/ui/Modifier;)V", "editor_output_status_card", "title", "subtitle", "running", "sheet_progress", "on_click", "on_drag_start", "on_drag", "Lkotlin/Function1;", "on_drag_end", "editor_output_tabs", "selected_tab", "Lcom/jmwl/gostudio/ui/screens/editor/editor_output_tab;", "on_select", "editor_output_content", "editor_output_line_list", "lines", "", "Lcom/jmwl/gostudio/ui/screens/editor/editor_output_line;", "revision", "editor_output_empty_state", "tab", "share_editor_output_text", "context", "Landroid/content/Context;", "text", "editor_output_floating_button", "icon", "Landroidx/compose/ui/graphics/vector/ImageVector;", "content_description", "background", "Landroidx/compose/ui/graphics/Color;", "tint", "editor_output_floating_button-OoHUuok", "(Landroidx/compose/ui/graphics/vector/ImageVector;Ljava/lang/String;JJLkotlin/jvm/functions/Function0;)V", "editor_running_border", "color", "rotation", "editor_running_border-9LQNqLg", "(Landroidx/compose/ui/Modifier;ZJF)Landroidx/compose/ui/Modifier;", "app_debug"})
public final class Editor_output_panelKt {
    private static final int editor_output_max_lines = 1000;
    @org.jetbrains.annotations.NotNull()
    private static final java.time.format.DateTimeFormatter editor_log_time_formatter = null;
    
    @androidx.compose.runtime.Composable()
    @org.jetbrains.annotations.NotNull()
    public static final com.jmwl.gostudio.ui.screens.editor.editor_output_panel_state remember_editor_output_panel_state() {
        return null;
    }
    
    @androidx.compose.runtime.Composable()
    public static final void editor_output_bottom_sheet_scaffold(@org.jetbrains.annotations.NotNull()
    com.jmwl.gostudio.ui.screens.editor.editor_output_panel_state state, @org.jetbrains.annotations.NotNull()
    com.jmwl.gostudio.ui.terminal.terminal_state terminal_state, @org.jetbrains.annotations.NotNull()
    java.lang.String terminal_cwd, @org.jetbrains.annotations.NotNull()
    java.util.Map<java.lang.String, java.lang.String> terminal_extra_environment, boolean show_symbol_bar, @org.jetbrains.annotations.NotNull()
    androidx.compose.runtime.internal.ComposableFunction0<kotlin.Unit> symbol_bar, @org.jetbrains.annotations.NotNull()
    androidx.compose.ui.Modifier modifier, @org.jetbrains.annotations.NotNull()
    androidx.compose.runtime.internal.ComposableFunction2<? super androidx.compose.foundation.layout.PaddingValues, ? super java.lang.Float, kotlin.Unit> content) {
    }
    
    @androidx.compose.runtime.Composable()
    private static final void editor_output_status_card(java.lang.String title, java.lang.String subtitle, boolean running, float sheet_progress, kotlin.jvm.functions.Function0<kotlin.Unit> on_click, kotlin.jvm.functions.Function0<kotlin.Unit> on_drag_start, kotlin.jvm.functions.Function1<? super java.lang.Float, kotlin.Unit> on_drag, kotlin.jvm.functions.Function0<kotlin.Unit> on_drag_end, androidx.compose.ui.Modifier modifier) {
    }
    
    @androidx.compose.runtime.Composable()
    private static final void editor_output_tabs(com.jmwl.gostudio.ui.screens.editor.editor_output_tab selected_tab, kotlin.jvm.functions.Function1<? super com.jmwl.gostudio.ui.screens.editor.editor_output_tab, kotlin.Unit> on_select) {
    }
    
    @androidx.compose.runtime.Composable()
    private static final void editor_output_content(com.jmwl.gostudio.ui.screens.editor.editor_output_panel_state state, com.jmwl.gostudio.ui.terminal.terminal_state terminal_state, java.lang.String terminal_cwd, java.util.Map<java.lang.String, java.lang.String> terminal_extra_environment, androidx.compose.ui.Modifier modifier) {
    }
    
    @androidx.compose.runtime.Composable()
    private static final void editor_output_line_list(java.util.List<com.jmwl.gostudio.ui.screens.editor.editor_output_line> lines, int revision, androidx.compose.ui.Modifier modifier) {
    }
    
    @androidx.compose.runtime.Composable()
    private static final void editor_output_empty_state(com.jmwl.gostudio.ui.screens.editor.editor_output_tab tab, androidx.compose.ui.Modifier modifier) {
    }
    
    private static final void share_editor_output_text(android.content.Context context, java.lang.String title, java.lang.String text) {
    }
}