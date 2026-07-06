package com.jmwl.gostudio.ui.screens.editor;

@kotlin.Metadata(mv = {2, 3, 0}, k = 2, xi = 48, d1 = {"\u0000N\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0002\n\u0002\u0010\u000b\n\u0002\b\u0006\n\u0002\u0018\u0002\n\u0002\b\u0006\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\n\n\u0002\u0018\u0002\n\u0002\b\u0002\u001a\u00a4\u0002\u0010\u0000\u001a\u00020\u00012\u0006\u0010\u0002\u001a\u00020\u00032\u0006\u0010\u0004\u001a\u00020\u00032\u0006\u0010\u0005\u001a\u00020\u00062\u0006\u0010\u0007\u001a\u00020\u00062\u0006\u0010\b\u001a\u00020\u00062\u0006\u0010\t\u001a\u00020\u00062\u0006\u0010\n\u001a\u00020\u00062\u0006\u0010\u000b\u001a\u00020\u00062\u0012\u0010\f\u001a\u000e\u0012\u0004\u0012\u00020\u0003\u0012\u0004\u0012\u00020\u00010\r2\u0012\u0010\u000e\u001a\u000e\u0012\u0004\u0012\u00020\u0003\u0012\u0004\u0012\u00020\u00010\r2\u0012\u0010\u000f\u001a\u000e\u0012\u0004\u0012\u00020\u0006\u0012\u0004\u0012\u00020\u00010\r2\u0012\u0010\u0010\u001a\u000e\u0012\u0004\u0012\u00020\u0006\u0012\u0004\u0012\u00020\u00010\r2\u0012\u0010\u0011\u001a\u000e\u0012\u0004\u0012\u00020\u0006\u0012\u0004\u0012\u00020\u00010\r2\u0012\u0010\u0012\u001a\u000e\u0012\u0004\u0012\u00020\u0006\u0012\u0004\u0012\u00020\u00010\r2\f\u0010\u0013\u001a\b\u0012\u0004\u0012\u00020\u00010\u00142\f\u0010\u0015\u001a\b\u0012\u0004\u0012\u00020\u00010\u00142\f\u0010\u0016\u001a\b\u0012\u0004\u0012\u00020\u00010\u00142\f\u0010\u0017\u001a\b\u0012\u0004\u0012\u00020\u00010\u00142\f\u0010\u0018\u001a\b\u0012\u0004\u0012\u00020\u00010\u00142\u0012\u0010\u0019\u001a\u000e\u0012\u0004\u0012\u00020\u001a\u0012\u0004\u0012\u00020\u00010\r2\b\b\u0002\u0010\u001b\u001a\u00020\u001cH\u0007\u001a^\u0010\u001d\u001a\u00020\u00012\u0006\u0010\u001e\u001a\u00020\u00032\u0006\u0010\u001f\u001a\u00020\u00032\u0006\u0010\u0005\u001a\u00020\u00062\u0012\u0010 \u001a\u000e\u0012\u0004\u0012\u00020\u0003\u0012\u0004\u0012\u00020\u00010\r2\u0012\u0010\u000f\u001a\u000e\u0012\u0004\u0012\u00020\u0006\u0012\u0004\u0012\u00020\u00010\r2\n\b\u0002\u0010!\u001a\u0004\u0018\u00010\"2\b\b\u0002\u0010\u001b\u001a\u00020\u001cH\u0003\u001aM\u0010#\u001a\u00020\u00012\u0006\u0010\u001e\u001a\u00020\u00032\u0006\u0010\u001f\u001a\u00020\u00032\u0012\u0010 \u001a\u000e\u0012\u0004\u0012\u00020\u0003\u0012\u0004\u0012\u00020\u00010\r2\b\b\u0002\u0010\u001b\u001a\u00020\u001c2\u0015\b\u0002\u0010$\u001a\u000f\u0012\u0004\u0012\u00020\u0001\u0018\u00010\u0014\u00a2\u0006\u0002\b%H\u0003\u001aV\u0010&\u001a\u00020\u00012\u0006\u0010\u001e\u001a\u00020\u00032\u0006\u0010\u001f\u001a\u00020\u00032\u0012\u0010 \u001a\u000e\u0012\u0004\u0012\u00020\u0003\u0012\u0004\u0012\u00020\u00010\r2\u0012\u0010\'\u001a\u000e\u0012\u0004\u0012\u00020\u0006\u0012\u0004\u0012\u00020\u00010\r2\n\b\u0002\u0010!\u001a\u0004\u0018\u00010\"2\b\b\u0002\u0010\u001b\u001a\u00020\u001cH\u0003\u001a&\u0010(\u001a\u00020\u00012\u0006\u0010)\u001a\u00020\u00062\u0006\u0010*\u001a\u00020\u00032\f\u0010+\u001a\b\u0012\u0004\u0012\u00020\u00010\u0014H\u0003\u001a&\u0010,\u001a\u00020\u00012\u0006\u0010-\u001a\u00020\u00032\u0006\u0010)\u001a\u00020\u00062\f\u0010+\u001a\b\u0012\u0004\u0012\u00020\u00010\u0014H\u0003\u001a8\u0010.\u001a\u00020\u00012\u0006\u0010/\u001a\u0002002\u0006\u00101\u001a\u00020\u00062\u0006\u0010*\u001a\u00020\u00032\f\u0010+\u001a\b\u0012\u0004\u0012\u00020\u00010\u00142\b\b\u0002\u0010\u001b\u001a\u00020\u001cH\u0003\u00a8\u00062"}, d2 = {"editor_search_panel", "", "query", "", "replacement", "expanded", "", "match_case", "whole_word", "regex", "has_match", "replace_enabled", "on_query_change", "Lkotlin/Function1;", "on_replacement_change", "on_expanded_change", "on_match_case_change", "on_whole_word_change", "on_regex_change", "on_previous", "Lkotlin/Function0;", "on_next", "on_replace_current", "on_replace_all", "on_close", "on_drag", "Landroidx/compose/ui/geometry/Offset;", "modifier", "Landroidx/compose/ui/Modifier;", "search_box_row", "value", "placeholder", "on_value_change", "focus_requester", "Landroidx/compose/ui/focus/FocusRequester;", "search_text_field", "trailing_content", "Landroidx/compose/runtime/Composable;", "search_plain_text_field", "on_focus_change", "search_replace_mode_button", "selected", "content_description", "on_click", "search_option_button", "label", "search_replace_button", "icon", "Landroidx/compose/ui/graphics/vector/ImageVector;", "enabled", "app_release"})
public final class Editor_search_panelKt {
    
    @androidx.compose.runtime.Composable()
    public static final void editor_search_panel(@org.jetbrains.annotations.NotNull()
    java.lang.String query, @org.jetbrains.annotations.NotNull()
    java.lang.String replacement, boolean expanded, boolean match_case, boolean whole_word, boolean regex, boolean has_match, boolean replace_enabled, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function1<? super java.lang.String, kotlin.Unit> on_query_change, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function1<? super java.lang.String, kotlin.Unit> on_replacement_change, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function1<? super java.lang.Boolean, kotlin.Unit> on_expanded_change, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function1<? super java.lang.Boolean, kotlin.Unit> on_match_case_change, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function1<? super java.lang.Boolean, kotlin.Unit> on_whole_word_change, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function1<? super java.lang.Boolean, kotlin.Unit> on_regex_change, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function0<kotlin.Unit> on_previous, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function0<kotlin.Unit> on_next, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function0<kotlin.Unit> on_replace_current, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function0<kotlin.Unit> on_replace_all, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function0<kotlin.Unit> on_close, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function1<? super androidx.compose.ui.geometry.Offset, kotlin.Unit> on_drag, @org.jetbrains.annotations.NotNull()
    androidx.compose.ui.Modifier modifier) {
    }
    
    @androidx.compose.runtime.Composable()
    private static final void search_box_row(java.lang.String value, java.lang.String placeholder, boolean expanded, kotlin.jvm.functions.Function1<? super java.lang.String, kotlin.Unit> on_value_change, kotlin.jvm.functions.Function1<? super java.lang.Boolean, kotlin.Unit> on_expanded_change, androidx.compose.ui.focus.FocusRequester focus_requester, androidx.compose.ui.Modifier modifier) {
    }
    
    @androidx.compose.runtime.Composable()
    private static final void search_text_field(java.lang.String value, java.lang.String placeholder, kotlin.jvm.functions.Function1<? super java.lang.String, kotlin.Unit> on_value_change, androidx.compose.ui.Modifier modifier, androidx.compose.runtime.internal.ComposableFunction0<kotlin.Unit> trailing_content) {
    }
    
    @androidx.compose.runtime.Composable()
    private static final void search_plain_text_field(java.lang.String value, java.lang.String placeholder, kotlin.jvm.functions.Function1<? super java.lang.String, kotlin.Unit> on_value_change, kotlin.jvm.functions.Function1<? super java.lang.Boolean, kotlin.Unit> on_focus_change, androidx.compose.ui.focus.FocusRequester focus_requester, androidx.compose.ui.Modifier modifier) {
    }
    
    @androidx.compose.runtime.Composable()
    private static final void search_replace_mode_button(boolean selected, java.lang.String content_description, kotlin.jvm.functions.Function0<kotlin.Unit> on_click) {
    }
    
    @androidx.compose.runtime.Composable()
    private static final void search_option_button(java.lang.String label, boolean selected, kotlin.jvm.functions.Function0<kotlin.Unit> on_click) {
    }
    
    @androidx.compose.runtime.Composable()
    private static final void search_replace_button(androidx.compose.ui.graphics.vector.ImageVector icon, boolean enabled, java.lang.String content_description, kotlin.jvm.functions.Function0<kotlin.Unit> on_click, androidx.compose.ui.Modifier modifier) {
    }
}