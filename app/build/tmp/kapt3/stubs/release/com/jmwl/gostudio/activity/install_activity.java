package com.jmwl.gostudio.activity;

@kotlin.Metadata(mv = {2, 3, 0}, k = 1, xi = 48, d1 = {"\u0000>\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010 \n\u0002\u0010\u000e\n\u0002\b\u0007\n\u0002\u0010\u000b\n\u0002\b\u000b\n\u0002\u0010\u0007\n\u0002\b\b\n\u0002\u0018\u0002\n\u0002\b\u000b\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\t\u0018\u00002\u00020\u0001B\u0007\u00a2\u0006\u0004\b\u0002\u0010\u0003J\u0012\u0010.\u001a\u00020/2\b\u00100\u001a\u0004\u0018\u000101H\u0014J\u0010\u00102\u001a\u00020/2\u0006\u00103\u001a\u00020\u0006H\u0002J\u0010\u00104\u001a\u00020/2\u0006\u00105\u001a\u00020\u0006H\u0002J\b\u00106\u001a\u00020/H\u0002J\b\u00107\u001a\u00020/H\u0002J\b\u00108\u001a\u00020/H\u0002J\b\u00109\u001a\u00020/H\u0002R7\u0010\u0007\u001a\b\u0012\u0004\u0012\u00020\u00060\u00052\f\u0010\u0004\u001a\b\u0012\u0004\u0012\u00020\u00060\u00058B@BX\u0082\u008e\u0002\u00a2\u0006\u0012\n\u0004\b\f\u0010\r\u001a\u0004\b\b\u0010\t\"\u0004\b\n\u0010\u000bR+\u0010\u000f\u001a\u00020\u000e2\u0006\u0010\u0004\u001a\u00020\u000e8B@BX\u0082\u008e\u0002\u00a2\u0006\u0012\n\u0004\b\u0013\u0010\r\u001a\u0004\b\u000f\u0010\u0010\"\u0004\b\u0011\u0010\u0012R+\u0010\u0014\u001a\u00020\u000e2\u0006\u0010\u0004\u001a\u00020\u000e8B@BX\u0082\u008e\u0002\u00a2\u0006\u0012\n\u0004\b\u0016\u0010\r\u001a\u0004\b\u0014\u0010\u0010\"\u0004\b\u0015\u0010\u0012R+\u0010\u0017\u001a\u00020\u000e2\u0006\u0010\u0004\u001a\u00020\u000e8B@BX\u0082\u008e\u0002\u00a2\u0006\u0012\n\u0004\b\u0019\u0010\r\u001a\u0004\b\u0017\u0010\u0010\"\u0004\b\u0018\u0010\u0012R+\u0010\u001b\u001a\u00020\u001a2\u0006\u0010\u0004\u001a\u00020\u001a8B@BX\u0082\u008e\u0002\u00a2\u0006\u0012\n\u0004\b \u0010!\u001a\u0004\b\u001c\u0010\u001d\"\u0004\b\u001e\u0010\u001fR\u0014\u0010\"\u001a\u00020#8BX\u0082\u0004\u00a2\u0006\u0006\u001a\u0004\b$\u0010%R\u0014\u0010&\u001a\u00020#8BX\u0082\u0004\u00a2\u0006\u0006\u001a\u0004\b\'\u0010%R\u0014\u0010(\u001a\u00020#8BX\u0082\u0004\u00a2\u0006\u0006\u001a\u0004\b)\u0010%R\u000e\u0010*\u001a\u00020\u0006X\u0082D\u00a2\u0006\u0002\n\u0000R\u000e\u0010+\u001a\u00020\u0006X\u0082D\u00a2\u0006\u0002\n\u0000R\u000e\u0010,\u001a\u00020\u0006X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0014\u0010-\u001a\b\u0012\u0004\u0012\u00020\u00060\u0005X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006:"}, d2 = {"Lcom/jmwl/gostudio/activity/install_activity;", "Landroidx/activity/ComponentActivity;", "<init>", "()V", "<set-?>", "", "", "logs", "getLogs", "()Ljava/util/List;", "setLogs", "(Ljava/util/List;)V", "logs$delegate", "Landroidx/compose/runtime/MutableState;", "", "is_downloading", "()Z", "set_downloading", "(Z)V", "is_downloading$delegate", "is_extracting", "set_extracting", "is_extracting$delegate", "is_configuring", "set_configuring", "is_configuring$delegate", "", "current_progress", "getCurrent_progress", "()F", "setCurrent_progress", "(F)V", "current_progress$delegate", "Landroidx/compose/runtime/MutableFloatState;", "home_dir_path", "Ljava/io/File;", "getHome_dir_path", "()Ljava/io/File;", "gostudio_dir_path", "getGostudio_dir_path", "ubuntu_base_dir_path", "getUbuntu_base_dir_path", "ubuntu_version", "expected_md5", "ubuntu_filename", "mirrors", "onCreate", "", "savedInstanceState", "Landroid/os/Bundle;", "add_log", "text", "add_proot_log", "line", "start_download", "configure_environment", "navigate_to_main", "export_logs", "app_release"})
public final class install_activity extends androidx.activity.ComponentActivity {
    @org.jetbrains.annotations.NotNull()
    private final androidx.compose.runtime.MutableState logs$delegate = null;
    @org.jetbrains.annotations.NotNull()
    private final androidx.compose.runtime.MutableState is_downloading$delegate = null;
    @org.jetbrains.annotations.NotNull()
    private final androidx.compose.runtime.MutableState is_extracting$delegate = null;
    @org.jetbrains.annotations.NotNull()
    private final androidx.compose.runtime.MutableState is_configuring$delegate = null;
    @org.jetbrains.annotations.NotNull()
    private final androidx.compose.runtime.MutableFloatState current_progress$delegate = null;
    @org.jetbrains.annotations.NotNull()
    private final java.lang.String ubuntu_version = "24.04.4";
    @org.jetbrains.annotations.NotNull()
    private final java.lang.String expected_md5 = "5acb2eb6fe98908f41bc4e9ac0014c91";
    @org.jetbrains.annotations.NotNull()
    private final java.lang.String ubuntu_filename = null;
    @org.jetbrains.annotations.NotNull()
    private final java.util.List<java.lang.String> mirrors = null;
    
    public install_activity() {
        super();
    }
    
    private final java.util.List<java.lang.String> getLogs() {
        return null;
    }
    
    private final void setLogs(java.util.List<java.lang.String> p0) {
    }
    
    private final boolean is_downloading() {
        return false;
    }
    
    private final void set_downloading(boolean p0) {
    }
    
    private final boolean is_extracting() {
        return false;
    }
    
    private final void set_extracting(boolean p0) {
    }
    
    private final boolean is_configuring() {
        return false;
    }
    
    private final void set_configuring(boolean p0) {
    }
    
    private final float getCurrent_progress() {
        return 0.0F;
    }
    
    private final void setCurrent_progress(float p0) {
    }
    
    private final java.io.File getHome_dir_path() {
        return null;
    }
    
    private final java.io.File getGostudio_dir_path() {
        return null;
    }
    
    private final java.io.File getUbuntu_base_dir_path() {
        return null;
    }
    
    @java.lang.Override()
    protected void onCreate(@org.jetbrains.annotations.Nullable()
    android.os.Bundle savedInstanceState) {
    }
    
    private final void add_log(java.lang.String text) {
    }
    
    private final void add_proot_log(java.lang.String line) {
    }
    
    private final void start_download() {
    }
    
    private final void configure_environment() {
    }
    
    private final void navigate_to_main() {
    }
    
    private final void export_logs() {
    }
}