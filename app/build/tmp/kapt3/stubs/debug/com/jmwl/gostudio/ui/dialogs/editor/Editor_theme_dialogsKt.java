package com.jmwl.gostudio.ui.dialogs.editor;

@kotlin.Metadata(mv = {2, 3, 0}, k = 2, xi = 48, d1 = {"\u0000*\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\f\n\u0002\u0010 \n\u0000\n\u0002\u0018\u0002\n\u0000\u001aB\u0010\u0000\u001a\u00020\u00012\u0006\u0010\u0002\u001a\u00020\u00032\u0006\u0010\u0004\u001a\u00020\u00032\u0006\u0010\u0005\u001a\u00020\u00032\f\u0010\u0006\u001a\b\u0012\u0004\u0012\u00020\u00010\u00072\u0012\u0010\b\u001a\u000e\u0012\u0004\u0012\u00020\u0003\u0012\u0004\u0012\u00020\u00010\tH\u0007\u001a&\u0010\n\u001a\u00020\u00012\b\u0010\u000b\u001a\u0004\u0018\u00010\u00032\u0012\u0010\f\u001a\u000e\u0012\u0004\u0012\u00020\u0003\u0012\u0004\u0012\u00020\u00010\tH\u0003\u001a&\u0010\r\u001a\u00020\u00012\b\u0010\u0005\u001a\u0004\u0018\u00010\u00032\u0012\u0010\f\u001a\u000e\u0012\u0004\u0012\u00020\u0003\u0012\u0004\u0012\u00020\u00010\tH\u0003\u001a\u001a\u0010\u000e\u001a\u00020\u00032\u0006\u0010\u000f\u001a\u00020\u00032\b\u0010\u0010\u001a\u0004\u0018\u00010\u0003H\u0002\u001a\u0018\u0010\u0011\u001a\u00020\u00032\u0006\u0010\u0005\u001a\u00020\u00032\u0006\u0010\u0012\u001a\u00020\u0003H\u0002\u001a$\u0010\u0013\u001a\u00020\u00012\f\u0010\u0006\u001a\b\u0012\u0004\u0012\u00020\u00010\u00072\f\u0010\u0014\u001a\b\u0012\u0004\u0012\u00020\u00010\u0007H\u0007\"\u0014\u0010\u0015\u001a\b\u0012\u0004\u0012\u00020\u00030\u0016X\u0082\u0004\u00a2\u0006\u0002\n\u0000\" \u0010\u0017\u001a\u0014\u0012\u0010\u0012\u000e\u0012\u0004\u0012\u00020\u0003\u0012\u0004\u0012\u00020\u00030\u00180\u0016X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0019"}, d2 = {"editor_theme_color_dialog", "", "title", "", "key", "value", "on_dismiss", "Lkotlin/Function0;", "on_save", "Lkotlin/Function1;", "editor_theme_color_palette", "selected", "on_select", "editor_theme_alpha_palette", "apply_existing_alpha", "color_value", "current", "apply_alpha", "alpha", "editor_theme_reset_dialog", "on_confirm", "editor_theme_quick_colors", "", "editor_theme_alpha_options", "Lkotlin/Pair;", "app_debug"})
public final class Editor_theme_dialogsKt {
    @org.jetbrains.annotations.NotNull()
    private static final java.util.List<java.lang.String> editor_theme_quick_colors = null;
    @org.jetbrains.annotations.NotNull()
    private static final java.util.List<kotlin.Pair<java.lang.String, java.lang.String>> editor_theme_alpha_options = null;
    
    @kotlin.OptIn(markerClass = {androidx.compose.material3.ExperimentalMaterial3Api.class, androidx.compose.foundation.layout.ExperimentalLayoutApi.class})
    @androidx.compose.runtime.Composable()
    public static final void editor_theme_color_dialog(@org.jetbrains.annotations.NotNull()
    java.lang.String title, @org.jetbrains.annotations.NotNull()
    java.lang.String key, @org.jetbrains.annotations.NotNull()
    java.lang.String value, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function0<kotlin.Unit> on_dismiss, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function1<? super java.lang.String, kotlin.Unit> on_save) {
    }
    
    @kotlin.OptIn(markerClass = {androidx.compose.foundation.layout.ExperimentalLayoutApi.class})
    @androidx.compose.runtime.Composable()
    private static final void editor_theme_color_palette(java.lang.String selected, kotlin.jvm.functions.Function1<? super java.lang.String, kotlin.Unit> on_select) {
    }
    
    @kotlin.OptIn(markerClass = {androidx.compose.material3.ExperimentalMaterial3Api.class, androidx.compose.foundation.layout.ExperimentalLayoutApi.class})
    @androidx.compose.runtime.Composable()
    private static final void editor_theme_alpha_palette(java.lang.String value, kotlin.jvm.functions.Function1<? super java.lang.String, kotlin.Unit> on_select) {
    }
    
    private static final java.lang.String apply_existing_alpha(java.lang.String color_value, java.lang.String current) {
        return null;
    }
    
    private static final java.lang.String apply_alpha(java.lang.String value, java.lang.String alpha) {
        return null;
    }
    
    @androidx.compose.runtime.Composable()
    public static final void editor_theme_reset_dialog(@org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function0<kotlin.Unit> on_dismiss, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function0<kotlin.Unit> on_confirm) {
    }
}