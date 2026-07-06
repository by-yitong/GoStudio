package com.jmwl.gostudio.ui.screens.editor;

@kotlin.Metadata(mv = {2, 3, 0}, k = 2, xi = 48, d1 = {"\u0000j\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000b\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0010\u000e\n\u0002\b\u0006\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0010\u0007\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\b\n\u0002\b\f\u001aJ\u0010\u0000\u001a\u00020\u00012\u0006\u0010\u0002\u001a\u00020\u00032\u0012\u0010\u0004\u001a\u000e\u0012\u0004\u0012\u00020\u0003\u0012\u0004\u0012\u00020\u00010\u00052\f\u0010\u0006\u001a\b\u0012\u0004\u0012\u00020\u00010\u00072\f\u0010\b\u001a\b\u0012\u0004\u0012\u00020\u00010\u00072\b\b\u0002\u0010\t\u001a\u00020\nH\u0007\u001a\b\u0010\u000b\u001a\u00020\u0001H\u0003\u001a)\u0010\f\u001a\u00020\r2\u0006\u0010\u000e\u001a\u00020\u000f2\u0006\u0010\u0010\u001a\u00020\u000f2\b\b\u0002\u0010\u0011\u001a\u00020\u0012H\u0002\u00a2\u0006\u0004\b\u0013\u0010\u0014\u001a\u0010\u0010\u0015\u001a\u00020\u00012\u0006\u0010\u0016\u001a\u00020\u0017H\u0003\u001a<\u0010\u0018\u001a\u00020\u00012\u0006\u0010\u0019\u001a\u00020\u00172\b\b\u0002\u0010\u001a\u001a\u00020\r2\u0012\u0010\u001b\u001a\u000e\u0012\u0004\u0012\u00020\u0017\u0012\u0004\u0012\u00020\u00010\u00052\f\u0010\u0006\u001a\b\u0012\u0004\u0012\u00020\u00010\u0007H\u0003\u001a\u008a\u0001\u0010\u001c\u001a\u00020\u00012\u0006\u0010\u001d\u001a\u00020\u001e2\u0006\u0010\u0016\u001a\u00020\u00172\u0006\u0010\u001f\u001a\u00020\u00172\u0006\u0010 \u001a\u00020\u000f2\u0018\u0010!\u001a\u0014\u0012\u0010\u0012\u000e\u0012\u0004\u0012\u00020\u0017\u0012\u0004\u0012\u00020\u00170#0\"2\u0006\u0010$\u001a\u00020\u00172\b\b\u0002\u0010\u001a\u001a\u00020\r2\u0012\u0010%\u001a\u000e\u0012\u0004\u0012\u00020\u000f\u0012\u0004\u0012\u00020\u00010\u00052\u0012\u0010&\u001a\u000e\u0012\u0004\u0012\u00020\u0017\u0012\u0004\u0012\u00020\u00010\u00052\f\u0010\u0006\u001a\b\u0012\u0004\u0012\u00020\u00010\u0007H\u0003\u001ad\u0010\'\u001a\u00020\u00012\u0006\u0010\u001d\u001a\u00020\u001e2\u0006\u0010\u0016\u001a\u00020\u00172\u0006\u0010\u001f\u001a\u00020\u00172\u0006\u0010(\u001a\u00020)2\f\u0010*\u001a\b\u0012\u0004\u0012\u00020)0+2\u0006\u0010,\u001a\u00020-2\u0006\u0010.\u001a\u00020\u00172\b\b\u0002\u0010\u001a\u001a\u00020\r2\u0012\u0010&\u001a\u000e\u0012\u0004\u0012\u00020)\u0012\u0004\u0012\u00020\u00010\u0005H\u0003\u001a2\u0010/\u001a\u00020\u00012\u0006\u0010\u0016\u001a\u00020\u00172\u0006\u00100\u001a\u00020\u000f2\n\b\u0002\u0010\u001d\u001a\u0004\u0018\u00010\u001e2\f\u00101\u001a\b\u0012\u0004\u0012\u00020\u00010\u0007H\u0003\u001a.\u00102\u001a\u00020\u00012\u0006\u0010\u001d\u001a\u00020\u001e2\u0006\u0010\u0016\u001a\u00020\u00172\u0006\u0010\u001f\u001a\u00020\u00172\f\u00101\u001a\b\u0012\u0004\u0012\u00020\u00010\u0007H\u0003\u001aF\u00103\u001a\u00020\u00012\u0006\u0010\u001d\u001a\u00020\u001e2\u0006\u0010\u0016\u001a\u00020\u00172\u0006\u0010\u001f\u001a\u00020\u00172\u0006\u00104\u001a\u00020\u000f2\b\b\u0002\u0010\u001a\u001a\u00020\r2\u0012\u00105\u001a\u000e\u0012\u0004\u0012\u00020\u000f\u0012\u0004\u0012\u00020\u00010\u0005H\u0003\u001a\u0018\u00106\u001a\u00020\u00012\u0006\u0010\u001d\u001a\u00020\u001e2\u0006\u0010\u0016\u001a\u00020\u0017H\u0003\u001a\u0010\u00107\u001a\u00020\u00012\u0006\u00104\u001a\u00020\u000fH\u0003\u001a\u0010\u00108\u001a\u00020\u00172\u0006\u0010(\u001a\u00020)H\u0002\u00a8\u00069"}, d2 = {"editor_settings_panel", "", "settings", "Lcom/jmwl/gostudio/editor/model/editor_settings_state;", "on_settings_change", "Lkotlin/Function1;", "on_import_font", "Lkotlin/Function0;", "on_open_theme_settings", "modifier", "Landroidx/compose/ui/Modifier;", "editor_settings_group_divider", "editor_settings_group_item_shape", "Landroidx/compose/foundation/shape/RoundedCornerShape;", "is_top", "", "is_bottom", "radius", "Landroidx/compose/ui/unit/Dp;", "editor_settings_group_item_shape-wH6b6FI", "(ZZF)Landroidx/compose/foundation/shape/RoundedCornerShape;", "editor_settings_group_title", "title", "", "editor_settings_font_card", "selected_font", "shape", "on_font_change", "editor_settings_expandable_options_card", "icon", "Landroidx/compose/ui/graphics/vector/ImageVector;", "description", "expanded", "options", "", "Lkotlin/Pair;", "selected_value", "on_expanded_change", "on_value_change", "editor_settings_slider_card", "value", "", "value_range", "Lkotlin/ranges/ClosedFloatingPointRange;", "steps", "", "value_label", "editor_settings_option_row", "selected", "on_click", "editor_settings_navigation_card", "editor_settings_switch_card", "checked", "on_checked_change", "editor_settings_icon", "editor_settings_small_switch", "format_editor_font_size", "app_debug"})
public final class Editor_settings_panelKt {
    
    @androidx.compose.runtime.Composable()
    public static final void editor_settings_panel(@org.jetbrains.annotations.NotNull()
    com.jmwl.gostudio.editor.model.editor_settings_state settings, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function1<? super com.jmwl.gostudio.editor.model.editor_settings_state, kotlin.Unit> on_settings_change, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function0<kotlin.Unit> on_import_font, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function0<kotlin.Unit> on_open_theme_settings, @org.jetbrains.annotations.NotNull()
    androidx.compose.ui.Modifier modifier) {
    }
    
    @androidx.compose.runtime.Composable()
    private static final void editor_settings_group_divider() {
    }
    
    @androidx.compose.runtime.Composable()
    private static final void editor_settings_group_title(java.lang.String title) {
    }
    
    @androidx.compose.runtime.Composable()
    private static final void editor_settings_font_card(java.lang.String selected_font, androidx.compose.foundation.shape.RoundedCornerShape shape, kotlin.jvm.functions.Function1<? super java.lang.String, kotlin.Unit> on_font_change, kotlin.jvm.functions.Function0<kotlin.Unit> on_import_font) {
    }
    
    @androidx.compose.runtime.Composable()
    private static final void editor_settings_expandable_options_card(androidx.compose.ui.graphics.vector.ImageVector icon, java.lang.String title, java.lang.String description, boolean expanded, java.util.List<kotlin.Pair<java.lang.String, java.lang.String>> options, java.lang.String selected_value, androidx.compose.foundation.shape.RoundedCornerShape shape, kotlin.jvm.functions.Function1<? super java.lang.Boolean, kotlin.Unit> on_expanded_change, kotlin.jvm.functions.Function1<? super java.lang.String, kotlin.Unit> on_value_change, kotlin.jvm.functions.Function0<kotlin.Unit> on_import_font) {
    }
    
    @androidx.compose.runtime.Composable()
    private static final void editor_settings_slider_card(androidx.compose.ui.graphics.vector.ImageVector icon, java.lang.String title, java.lang.String description, float value, kotlin.ranges.ClosedFloatingPointRange<java.lang.Float> value_range, int steps, java.lang.String value_label, androidx.compose.foundation.shape.RoundedCornerShape shape, kotlin.jvm.functions.Function1<? super java.lang.Float, kotlin.Unit> on_value_change) {
    }
    
    @androidx.compose.runtime.Composable()
    private static final void editor_settings_option_row(java.lang.String title, boolean selected, androidx.compose.ui.graphics.vector.ImageVector icon, kotlin.jvm.functions.Function0<kotlin.Unit> on_click) {
    }
    
    @androidx.compose.runtime.Composable()
    private static final void editor_settings_navigation_card(androidx.compose.ui.graphics.vector.ImageVector icon, java.lang.String title, java.lang.String description, kotlin.jvm.functions.Function0<kotlin.Unit> on_click) {
    }
    
    @androidx.compose.runtime.Composable()
    private static final void editor_settings_switch_card(androidx.compose.ui.graphics.vector.ImageVector icon, java.lang.String title, java.lang.String description, boolean checked, androidx.compose.foundation.shape.RoundedCornerShape shape, kotlin.jvm.functions.Function1<? super java.lang.Boolean, kotlin.Unit> on_checked_change) {
    }
    
    @androidx.compose.runtime.Composable()
    private static final void editor_settings_icon(androidx.compose.ui.graphics.vector.ImageVector icon, java.lang.String title) {
    }
    
    @androidx.compose.runtime.Composable()
    private static final void editor_settings_small_switch(boolean checked) {
    }
    
    private static final java.lang.String format_editor_font_size(float value) {
        return null;
    }
}