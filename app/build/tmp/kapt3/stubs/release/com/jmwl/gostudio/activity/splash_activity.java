package com.jmwl.gostudio.activity;

@kotlin.Metadata(mv = {2, 3, 0}, k = 1, xi = 48, d1 = {"\u0000J\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\u000b\n\u0002\b\u0002\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\b\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0010\b\n\u0000\n\u0002\u0010\u0011\n\u0002\u0010\u000e\n\u0000\n\u0002\u0010\u0015\n\u0002\b\u0003\b\u0007\u0018\u00002\u00020\u0001B\u0007\u00a2\u0006\u0004\b\u0002\u0010\u0003J\u0012\u0010\u0007\u001a\u00020\b2\b\u0010\t\u001a\u0004\u0018\u00010\nH\u0014J\b\u0010\u000b\u001a\u00020\bH\u0002J\b\u0010\f\u001a\u00020\u0005H\u0002J\b\u0010\r\u001a\u00020\bH\u0002J\b\u0010\u000e\u001a\u00020\bH\u0002J\b\u0010\u000f\u001a\u00020\bH\u0002J\b\u0010\u0010\u001a\u00020\bH\u0007J\u0010\u0010\u0011\u001a\u00020\b2\u0006\u0010\u0012\u001a\u00020\u0013H\u0007J\b\u0010\u0014\u001a\u00020\bH\u0007J\b\u0010\u0015\u001a\u00020\bH\u0007J\b\u0010\u0016\u001a\u00020\bH\u0002J$\u0010\u0017\u001a\u00020\b2\f\u0010\u0018\u001a\b\u0012\u0004\u0012\u00020\b0\u00192\f\u0010\u001a\u001a\b\u0012\u0004\u0012\u00020\b0\u0019H\u0002J\b\u0010\u001b\u001a\u00020\bH\u0002J+\u0010\u001c\u001a\u00020\b2\u0006\u0010\u001d\u001a\u00020\u001e2\f\u0010\u001f\u001a\b\u0012\u0004\u0012\u00020!0 2\u0006\u0010\"\u001a\u00020#H\u0017\u00a2\u0006\u0002\u0010$J\b\u0010%\u001a\u00020\bH\u0014R\u000e\u0010\u0004\u001a\u00020\u0005X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0006\u001a\u00020\u0005X\u0082\u000e\u00a2\u0006\u0002\n\u0000\u00a8\u0006&"}, d2 = {"Lcom/jmwl/gostudio/activity/splash_activity;", "Landroidx/activity/ComponentActivity;", "<init>", "()V", "has_navigated", "", "is_splash_ready", "onCreate", "", "savedInstanceState", "Landroid/os/Bundle;", "check_and_request_permission", "check_permission", "check_and_navigate", "navigate_to_main", "navigate_to_install", "request_permission_with_check", "show_rationale", "request", "Lpermissions/dispatcher/PermissionRequest;", "on_permissions_denied", "on_never_ask_again", "show_manage_storage_dialog", "show_rationale_dialog", "on_confirm", "Lkotlin/Function0;", "on_deny", "show_permission_denied_dialog", "onRequestPermissionsResult", "requestCode", "", "permissions", "", "", "grantResults", "", "(I[Ljava/lang/String;[I)V", "onResume", "app_release"})
@permissions.dispatcher.RuntimePermissions()
public final class splash_activity extends androidx.activity.ComponentActivity {
    private boolean has_navigated = false;
    private boolean is_splash_ready = false;
    
    public splash_activity() {
        super();
    }
    
    @java.lang.Override()
    protected void onCreate(@org.jetbrains.annotations.Nullable()
    android.os.Bundle savedInstanceState) {
    }
    
    private final void check_and_request_permission() {
    }
    
    private final boolean check_permission() {
        return false;
    }
    
    private final void check_and_navigate() {
    }
    
    private final void navigate_to_main() {
    }
    
    private final void navigate_to_install() {
    }
    
    @permissions.dispatcher.NeedsPermission(value = {"android.permission.WRITE_EXTERNAL_STORAGE", "android.permission.READ_EXTERNAL_STORAGE"})
    public final void request_permission_with_check() {
    }
    
    @permissions.dispatcher.OnShowRationale(value = {"android.permission.WRITE_EXTERNAL_STORAGE", "android.permission.READ_EXTERNAL_STORAGE"})
    public final void show_rationale(@org.jetbrains.annotations.NotNull()
    permissions.dispatcher.PermissionRequest request) {
    }
    
    @permissions.dispatcher.OnPermissionDenied(value = {"android.permission.WRITE_EXTERNAL_STORAGE", "android.permission.READ_EXTERNAL_STORAGE"})
    public final void on_permissions_denied() {
    }
    
    @permissions.dispatcher.OnNeverAskAgain(value = {"android.permission.WRITE_EXTERNAL_STORAGE", "android.permission.READ_EXTERNAL_STORAGE"})
    public final void on_never_ask_again() {
    }
    
    private final void show_manage_storage_dialog() {
    }
    
    private final void show_rationale_dialog(kotlin.jvm.functions.Function0<kotlin.Unit> on_confirm, kotlin.jvm.functions.Function0<kotlin.Unit> on_deny) {
    }
    
    private final void show_permission_denied_dialog() {
    }
    
    @java.lang.Override()
    @kotlin.Suppress(names = {"DEPRECATION"})
    @java.lang.Deprecated()
    public void onRequestPermissionsResult(int requestCode, @org.jetbrains.annotations.NotNull()
    java.lang.String[] permissions, @org.jetbrains.annotations.NotNull()
    int[] grantResults) {
    }
    
    @java.lang.Override()
    protected void onResume() {
    }
}