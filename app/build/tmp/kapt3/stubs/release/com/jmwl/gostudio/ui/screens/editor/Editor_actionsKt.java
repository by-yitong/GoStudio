package com.jmwl.gostudio.ui.screens.editor;

@kotlin.Metadata(mv = {2, 3, 0}, k = 2, xi = 48, d1 = {"\u0000T\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0010\b\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u000b\n\u0002\b\u0007\n\u0002\u0018\u0002\n\u0002\b\u0006\n\u0002\u0010\u000e\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\u001a\"\u0010\u0000\u001a\u00020\u00012\u0006\u0010\u0002\u001a\u00020\u00032\u0006\u0010\u0004\u001a\u00020\u00032\b\b\u0002\u0010\u0005\u001a\u00020\u0006H\u0007\u001a\u0082\u0001\u0010\u0007\u001a\u00020\u00012\u0006\u0010\b\u001a\u00020\t2\u0006\u0010\n\u001a\u00020\t2\u0006\u0010\u000b\u001a\u00020\t2\u0006\u0010\f\u001a\u00020\t2\u0006\u0010\r\u001a\u00020\t2\u0006\u0010\u000e\u001a\u00020\t2\u0006\u0010\u000f\u001a\u00020\t2\f\u0010\u0010\u001a\b\u0012\u0004\u0012\u00020\u00010\u00112\f\u0010\u0012\u001a\b\u0012\u0004\u0012\u00020\u00010\u00112\f\u0010\u0013\u001a\b\u0012\u0004\u0012\u00020\u00010\u00112\f\u0010\u0014\u001a\b\u0012\u0004\u0012\u00020\u00010\u00112\b\b\u0002\u0010\u0005\u001a\u00020\u0006H\u0007\u001a=\u0010\u0015\u001a\u00020\u00012\u0006\u0010\u0016\u001a\u00020\u00032\u0006\u0010\u0017\u001a\u00020\u00182\u0006\u0010\u0019\u001a\u00020\t2\u0006\u0010\u001a\u001a\u00020\u001b2\f\u0010\u001c\u001a\b\u0012\u0004\u0012\u00020\u00010\u0011H\u0003\u00a2\u0006\u0004\b\u001d\u0010\u001e\u001a=\u0010\u001f\u001a\u00020\u00012\u0006\u0010 \u001a\u00020!2\u0006\u0010\u0017\u001a\u00020\u00182\u0006\u0010\u0019\u001a\u00020\t2\u0006\u0010\u001a\u001a\u00020\u001b2\f\u0010\u001c\u001a\b\u0012\u0004\u0012\u00020\u00010\u0011H\u0003\u00a2\u0006\u0004\b\"\u0010#\u001a\u001c\u0010$\u001a\u00020\u00012\u0012\u0010%\u001a\u000e\u0012\u0004\u0012\u00020\u0018\u0012\u0004\u0012\u00020\u00010&H\u0007\u001a\u0010\u0010\'\u001a\u00020(2\u0006\u0010)\u001a\u00020*H\u0002\u001a,\u0010+\u001a\u00020\u00012\u0006\u0010,\u001a\u00020\u00182\u0006\u0010)\u001a\u00020*2\u0012\u0010%\u001a\u000e\u0012\u0004\u0012\u00020\u0018\u0012\u0004\u0012\u00020\u00010&H\u0003\u00a8\u0006-"}, d2 = {"cursor_chip", "", "line", "", "column", "modifier", "Landroidx/compose/ui/Modifier;", "editor_floating_actions", "can_undo", "", "can_redo", "has_changes", "can_format", "format_selection", "show_format", "show_save", "on_undo", "Lkotlin/Function0;", "on_redo", "on_format", "on_save", "compact_floating_painter_button", "icon_res", "content_description", "", "enabled", "tint", "Landroidx/compose/ui/graphics/Color;", "on_click", "compact_floating_painter_button-42QJj7c", "(ILjava/lang/String;ZJLkotlin/jvm/functions/Function0;)V", "compact_floating_icon_button", "icon", "Landroidx/compose/ui/graphics/vector/ImageVector;", "compact_floating_icon_button-42QJj7c", "(Landroidx/compose/ui/graphics/vector/ImageVector;Ljava/lang/String;ZJLkotlin/jvm/functions/Function0;)V", "editor_symbol_bar", "on_insert", "Lkotlin/Function1;", "editor_symbol_button_shape", "Landroidx/compose/foundation/shape/RoundedCornerShape;", "position", "Lcom/jmwl/gostudio/ui/screens/editor/symbol_button_position;", "editor_symbol_button", "symbol", "app_release"})
public final class Editor_actionsKt {
    
    @androidx.compose.runtime.Composable()
    public static final void cursor_chip(int line, int column, @org.jetbrains.annotations.NotNull()
    androidx.compose.ui.Modifier modifier) {
    }
    
    @androidx.compose.runtime.Composable()
    public static final void editor_floating_actions(boolean can_undo, boolean can_redo, boolean has_changes, boolean can_format, boolean format_selection, boolean show_format, boolean show_save, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function0<kotlin.Unit> on_undo, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function0<kotlin.Unit> on_redo, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function0<kotlin.Unit> on_format, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function0<kotlin.Unit> on_save, @org.jetbrains.annotations.NotNull()
    androidx.compose.ui.Modifier modifier) {
    }
    
    @androidx.compose.runtime.Composable()
    public static final void editor_symbol_bar(@org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function1<? super java.lang.String, kotlin.Unit> on_insert) {
    }
    
    private static final androidx.compose.foundation.shape.RoundedCornerShape editor_symbol_button_shape(com.jmwl.gostudio.ui.screens.editor.symbol_button_position position) {
        return null;
    }
    
    @androidx.compose.runtime.Composable()
    private static final void editor_symbol_button(java.lang.String symbol, com.jmwl.gostudio.ui.screens.editor.symbol_button_position position, kotlin.jvm.functions.Function1<? super java.lang.String, kotlin.Unit> on_insert) {
    }
}