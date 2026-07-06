package com.jmwl.gostudio.activity;

@kotlin.Metadata(mv = {2, 3, 0}, k = 1, xi = 48, d1 = {"\u0000r\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0002\b\u0007\n\u0002\u0018\u0002\n\u0002\b\u0006\n\u0002\u0010\u000e\n\u0002\b\b\n\u0002\u0018\u0002\n\u0002\b\u0006\n\u0002\u0010\u0007\n\u0002\b\u0006\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0002\b\u0007\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0017\n\u0002\u0010\u000b\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\b\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0000\u0018\u00002\u00020\u0001B\u0007\u00a2\u0006\u0004\b\u0002\u0010\u0003J\u0012\u00108\u001a\u0002092\b\u0010:\u001a\u0004\u0018\u00010;H\u0014J\b\u0010<\u001a\u000209H\u0002J\u000e\u0010=\u001a\u000209H\u0082@\u00a2\u0006\u0002\u0010>J\u000e\u0010?\u001a\u000209H\u0082@\u00a2\u0006\u0002\u0010>J\b\u0010@\u001a\u000209H\u0002J\u0010\u0010A\u001a\u0002092\u0006\u0010B\u001a\u00020\u001eH\u0002J\u0010\u0010C\u001a\u0002092\u0006\u0010D\u001a\u00020%H\u0002J\u0010\u0010E\u001a\u0002092\u0006\u0010F\u001a\u00020\u0006H\u0002J\u0010\u0010G\u001a\u0002092\u0006\u0010F\u001a\u00020\u0006H\u0002J@\u0010H\u001a\u0002092\u0006\u0010I\u001a\u00020\u00152\u0006\u0010J\u001a\u00020\u00152\u0006\u0010K\u001a\u00020\u00152\u0006\u0010L\u001a\u00020\u00152\u0006\u0010M\u001a\u00020\u00152\u0006\u0010N\u001a\u00020\u00152\u0006\u0010O\u001a\u00020\u0015H\u0002J\u0010\u0010P\u001a\u0002092\u0006\u0010J\u001a\u00020\u0015H\u0002J\u0018\u0010Q\u001a\u0002092\u0006\u0010I\u001a\u00020\u00152\u0006\u0010J\u001a\u00020\u0015H\u0002J>\u0010R\u001a\u00020S2\u0006\u0010T\u001a\u00020,2\u0012\u0010U\u001a\u000e\u0012\u0004\u0012\u00020\u0015\u0012\u0004\u0012\u0002090V2\u0012\u0010W\u001a\u000e\u0012\u0004\u0012\u00020X\u0012\u0004\u0012\u0002090VH\u0082@\u00a2\u0006\u0002\u0010YJ\u0010\u0010Z\u001a\u0002092\u0006\u0010T\u001a\u00020,H\u0002J\f\u0010[\u001a\u00020\u0006*\u00020\\H\u0002R7\u0010\u0007\u001a\b\u0012\u0004\u0012\u00020\u00060\u00052\f\u0010\u0004\u001a\b\u0012\u0004\u0012\u00020\u00060\u00058B@BX\u0082\u008e\u0002\u00a2\u0006\u0012\n\u0004\b\f\u0010\r\u001a\u0004\b\b\u0010\t\"\u0004\b\n\u0010\u000bR+\u0010\u000f\u001a\u00020\u000e2\u0006\u0010\u0004\u001a\u00020\u000e8B@BX\u0082\u008e\u0002\u00a2\u0006\u0012\n\u0004\b\u0014\u0010\r\u001a\u0004\b\u0010\u0010\u0011\"\u0004\b\u0012\u0010\u0013R7\u0010\u0016\u001a\b\u0012\u0004\u0012\u00020\u00150\u00052\f\u0010\u0004\u001a\b\u0012\u0004\u0012\u00020\u00150\u00058B@BX\u0082\u008e\u0002\u00a2\u0006\u0012\n\u0004\b\u0019\u0010\r\u001a\u0004\b\u0017\u0010\t\"\u0004\b\u0018\u0010\u000bR7\u0010\u001a\u001a\b\u0012\u0004\u0012\u00020\u00150\u00052\f\u0010\u0004\u001a\b\u0012\u0004\u0012\u00020\u00150\u00058B@BX\u0082\u008e\u0002\u00a2\u0006\u0012\n\u0004\b\u001d\u0010\r\u001a\u0004\b\u001b\u0010\t\"\u0004\b\u001c\u0010\u000bR+\u0010\u001f\u001a\u00020\u001e2\u0006\u0010\u0004\u001a\u00020\u001e8B@BX\u0082\u008e\u0002\u00a2\u0006\u0012\n\u0004\b$\u0010\r\u001a\u0004\b \u0010!\"\u0004\b\"\u0010#R+\u0010&\u001a\u00020%2\u0006\u0010\u0004\u001a\u00020%8B@BX\u0082\u008e\u0002\u00a2\u0006\u0012\n\u0004\b+\u0010\r\u001a\u0004\b\'\u0010(\"\u0004\b)\u0010*R7\u0010-\u001a\b\u0012\u0004\u0012\u00020,0\u00052\f\u0010\u0004\u001a\b\u0012\u0004\u0012\u00020,0\u00058B@BX\u0082\u008e\u0002\u00a2\u0006\u0012\n\u0004\b0\u0010\r\u001a\u0004\b.\u0010\t\"\u0004\b/\u0010\u000bR/\u00102\u001a\u0004\u0018\u0001012\b\u0010\u0004\u001a\u0004\u0018\u0001018B@BX\u0082\u008e\u0002\u00a2\u0006\u0012\n\u0004\b7\u0010\r\u001a\u0004\b3\u00104\"\u0004\b5\u00106\u00a8\u0006]"}, d2 = {"Lcom/jmwl/gostudio/activity/main_activity;", "Landroidx/activity/ComponentActivity;", "<init>", "()V", "<set-?>", "", "Lcom/jmwl/gostudio/ui/screens/main/recent_project;", "recent_projects", "getRecent_projects", "()Ljava/util/List;", "setRecent_projects", "(Ljava/util/List;)V", "recent_projects$delegate", "Landroidx/compose/runtime/MutableState;", "Lcom/jmwl/gostudio/ui/screens/main/main_tools_install_status;", "toolchain_status", "getToolchain_status", "()Lcom/jmwl/gostudio/ui/screens/main/main_tools_install_status;", "setToolchain_status", "(Lcom/jmwl/gostudio/ui/screens/main/main_tools_install_status;)V", "toolchain_status$delegate", "", "ndk_versions", "getNdk_versions", "setNdk_versions", "ndk_versions$delegate", "cmake_versions", "getCmake_versions", "setCmake_versions", "cmake_versions$delegate", "Lcom/jmwl/gostudio/ui/theme/app_theme_type;", "current_theme", "getCurrent_theme", "()Lcom/jmwl/gostudio/ui/theme/app_theme_type;", "setCurrent_theme", "(Lcom/jmwl/gostudio/ui/theme/app_theme_type;)V", "current_theme$delegate", "", "scale_value", "getScale_value", "()F", "setScale_value", "(F)V", "scale_value$delegate", "Lcom/jmwl/gostudio/ui/screens/main/toolchain_trigger;", "toolchain_tasks", "getToolchain_tasks", "setToolchain_tasks", "toolchain_tasks$delegate", "Lcom/jmwl/gostudio/ui/screens/main/toolchain_custom_install_request;", "custom_toolchain_dialog", "getCustom_toolchain_dialog", "()Lcom/jmwl/gostudio/ui/screens/main/toolchain_custom_install_request;", "setCustom_toolchain_dialog", "(Lcom/jmwl/gostudio/ui/screens/main/toolchain_custom_install_request;)V", "custom_toolchain_dialog$delegate", "onCreate", "", "savedInstanceState", "Landroid/os/Bundle;", "load_initial_data", "reload_recent_projects", "(Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "refresh_toolchain_status", "open_terminal", "set_theme", "theme", "set_scale", "scale", "open_recent_project", "project", "remove_recent_project", "create_project", "project_name", "project_path", "template_id", "ndk_version", "cmake_version", "android_platform", "cpp_standard", "open_project_path", "open_editor", "run_toolchain_task", "", "trigger", "on_log", "Lkotlin/Function1;", "on_progress", "", "(Lcom/jmwl/gostudio/ui/screens/main/toolchain_trigger;Lkotlin/jvm/functions/Function1;Lkotlin/jvm/functions/Function1;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "on_toolchain_task_success", "to_ui_recent_project", "Lcom/jmwl/gostudio/project/recent_project_info;", "app_release"})
public final class main_activity extends androidx.activity.ComponentActivity {
    @org.jetbrains.annotations.NotNull()
    private final androidx.compose.runtime.MutableState recent_projects$delegate = null;
    @org.jetbrains.annotations.NotNull()
    private final androidx.compose.runtime.MutableState toolchain_status$delegate = null;
    @org.jetbrains.annotations.NotNull()
    private final androidx.compose.runtime.MutableState ndk_versions$delegate = null;
    @org.jetbrains.annotations.NotNull()
    private final androidx.compose.runtime.MutableState cmake_versions$delegate = null;
    @org.jetbrains.annotations.NotNull()
    private final androidx.compose.runtime.MutableState current_theme$delegate = null;
    @org.jetbrains.annotations.NotNull()
    private final androidx.compose.runtime.MutableState scale_value$delegate = null;
    @org.jetbrains.annotations.NotNull()
    private final androidx.compose.runtime.MutableState toolchain_tasks$delegate = null;
    @org.jetbrains.annotations.NotNull()
    private final androidx.compose.runtime.MutableState custom_toolchain_dialog$delegate = null;
    
    public main_activity() {
        super();
    }
    
    private final java.util.List<com.jmwl.gostudio.ui.screens.main.recent_project> getRecent_projects() {
        return null;
    }
    
    private final void setRecent_projects(java.util.List<com.jmwl.gostudio.ui.screens.main.recent_project> p0) {
    }
    
    private final com.jmwl.gostudio.ui.screens.main.main_tools_install_status getToolchain_status() {
        return null;
    }
    
    private final void setToolchain_status(com.jmwl.gostudio.ui.screens.main.main_tools_install_status p0) {
    }
    
    private final java.util.List<java.lang.String> getNdk_versions() {
        return null;
    }
    
    private final void setNdk_versions(java.util.List<java.lang.String> p0) {
    }
    
    private final java.util.List<java.lang.String> getCmake_versions() {
        return null;
    }
    
    private final void setCmake_versions(java.util.List<java.lang.String> p0) {
    }
    
    private final com.jmwl.gostudio.ui.theme.app_theme_type getCurrent_theme() {
        return null;
    }
    
    private final void setCurrent_theme(com.jmwl.gostudio.ui.theme.app_theme_type p0) {
    }
    
    private final float getScale_value() {
        return 0.0F;
    }
    
    private final void setScale_value(float p0) {
    }
    
    private final java.util.List<com.jmwl.gostudio.ui.screens.main.toolchain_trigger> getToolchain_tasks() {
        return null;
    }
    
    private final void setToolchain_tasks(java.util.List<com.jmwl.gostudio.ui.screens.main.toolchain_trigger> p0) {
    }
    
    private final com.jmwl.gostudio.ui.screens.main.toolchain_custom_install_request getCustom_toolchain_dialog() {
        return null;
    }
    
    private final void setCustom_toolchain_dialog(com.jmwl.gostudio.ui.screens.main.toolchain_custom_install_request p0) {
    }
    
    @java.lang.Override()
    protected void onCreate(@org.jetbrains.annotations.Nullable()
    android.os.Bundle savedInstanceState) {
    }
    
    private final void load_initial_data() {
    }
    
    private final java.lang.Object reload_recent_projects(kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
    
    private final java.lang.Object refresh_toolchain_status(kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
    
    private final void open_terminal() {
    }
    
    private final void set_theme(com.jmwl.gostudio.ui.theme.app_theme_type theme) {
    }
    
    private final void set_scale(float scale) {
    }
    
    private final void open_recent_project(com.jmwl.gostudio.ui.screens.main.recent_project project) {
    }
    
    private final void remove_recent_project(com.jmwl.gostudio.ui.screens.main.recent_project project) {
    }
    
    private final void create_project(java.lang.String project_name, java.lang.String project_path, java.lang.String template_id, java.lang.String ndk_version, java.lang.String cmake_version, java.lang.String android_platform, java.lang.String cpp_standard) {
    }
    
    private final void open_project_path(java.lang.String project_path) {
    }
    
    private final void open_editor(java.lang.String project_name, java.lang.String project_path) {
    }
    
    private final java.lang.Object run_toolchain_task(com.jmwl.gostudio.ui.screens.main.toolchain_trigger trigger, kotlin.jvm.functions.Function1<? super java.lang.String, kotlin.Unit> on_log, kotlin.jvm.functions.Function1<? super java.lang.Integer, kotlin.Unit> on_progress, kotlin.coroutines.Continuation<? super java.lang.Boolean> $completion) {
        return null;
    }
    
    private final void on_toolchain_task_success(com.jmwl.gostudio.ui.screens.main.toolchain_trigger trigger) {
    }
    
    private final com.jmwl.gostudio.ui.screens.main.recent_project to_ui_recent_project(com.jmwl.gostudio.project.recent_project_info $this$to_ui_recent_project) {
        return null;
    }
}