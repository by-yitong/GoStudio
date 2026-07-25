package com.jmwl.gostudio.project;

@kotlin.Metadata(mv = {2, 3, 0}, k = 1, xi = 48, d1 = {"\u0000\u008c\u0001\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0003\n\u0002\u0010\b\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\"\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\r\n\u0002\u0010\u000b\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\n\n\u0002\u0010\u0002\n\u0002\b\u000e\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u000f\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0002\b\n\n\u0002\u0010!\n\u0002\u0018\u0002\n\u0002\b\t\n\u0002\u0010\t\n\u0000\b\u00c6\u0002\u0018\u00002\u00020\u0001B\t\b\u0002\u00a2\u0006\u0004\b\u0002\u0010\u0003JN\u0010\u0014\u001a\b\u0012\u0004\u0012\u00020\u00160\u00152\u0006\u0010\u0017\u001a\u00020\u00072\u0006\u0010\u0018\u001a\u00020\u00072\u0006\u0010\u0019\u001a\u00020\u00072\u0006\u0010\u001a\u001a\u00020\u00072\u0006\u0010\u001b\u001a\u00020\u00072\u0006\u0010\u001c\u001a\u00020\u00072\u0006\u0010\u001d\u001a\u00020\u0007H\u0086@\u00a2\u0006\u0004\b\u001e\u0010\u001fJ6\u0010 \u001a\b\u0012\u0004\u0012\u00020\u00160\u00152\u0006\u0010!\u001a\u00020\u00072\u0006\u0010\"\u001a\u00020\u00072\u0006\u0010\u0017\u001a\u00020\u00072\u0006\u0010#\u001a\u00020$H\u0086@\u00a2\u0006\u0004\b%\u0010&J:\u0010\'\u001a\u0014\u0012\u0010\u0012\u000e\u0012\u0004\u0012\u00020\u0007\u0012\u0004\u0012\u00020\u00070(0\u00152\u0006\u0010!\u001a\u00020\u00072\u0006\u0010\u0018\u001a\u00020\u00072\u0006\u0010)\u001a\u00020\u0007H\u0086@\u00a2\u0006\u0004\b*\u0010+J&\u0010,\u001a\b\u0012\u0004\u0012\u00020\u00160\u00152\u0006\u0010!\u001a\u00020\u00072\u0006\u0010\u0018\u001a\u00020\u0007H\u0086@\u00a2\u0006\u0004\b-\u0010.J2\u0010/\u001a\u0014\u0012\u0010\u0012\u000e\u0012\u0004\u0012\u00020\u0007\u0012\u0004\u0012\u00020\u00070(0\u00152\u0006\u0010!\u001a\u00020\u00072\u0006\u0010\u0018\u001a\u00020\u0007H\u0086@\u00a2\u0006\u0004\b0\u0010.J\u0010\u00101\u001a\u00020\u00072\u0006\u0010\u0017\u001a\u00020\u0007H\u0002J \u00102\u001a\u0002032\u0006\u00104\u001a\u00020\u00162\u0006\u0010\u0017\u001a\u00020\u00072\u0006\u0010\u0019\u001a\u00020\u0007H\u0002J\u0010\u00105\u001a\u00020\u00072\u0006\u0010\u0017\u001a\u00020\u0007H\u0002J\u0010\u00106\u001a\u00020\u00072\u0006\u0010\u0017\u001a\u00020\u0007H\u0002J\u0010\u00107\u001a\u00020\u00072\u0006\u0010\u0017\u001a\u00020\u0007H\u0002J\u0010\u00108\u001a\u00020\u00072\u0006\u0010\u0017\u001a\u00020\u0007H\u0002J\u0010\u00109\u001a\u00020\u00162\u0006\u0010:\u001a\u00020\u0016H\u0002J\u001b\u0010;\u001a\b\u0012\u0004\u0012\u0002030\u00152\u0006\u0010\u0018\u001a\u00020\u0007\u00a2\u0006\u0004\b<\u0010=J\u001b\u0010>\u001a\b\u0012\u0004\u0012\u0002030\u00152\u0006\u0010\u0018\u001a\u00020\u0007\u00a2\u0006\u0004\b?\u0010=J:\u0010@\u001a\u0002032\u0006\u00104\u001a\u00020\u00162\u0006\u0010\u0017\u001a\u00020\u00072\u0006\u0010\u001a\u001a\u00020\u00072\u0006\u0010\u001b\u001a\u00020\u00072\u0006\u0010\u0019\u001a\u00020\u00072\b\b\u0002\u0010A\u001a\u00020BH\u0002J\u000e\u0010C\u001a\u00020B2\u0006\u0010\u0018\u001a\u00020\u0007J\u000e\u0010D\u001a\u00020E2\u0006\u0010\u0018\u001a\u00020\u0007J#\u0010F\u001a\b\u0012\u0004\u0012\u0002030\u00152\u0006\u0010\u0018\u001a\u00020\u00072\u0006\u0010G\u001a\u00020E\u00a2\u0006\u0004\bH\u0010IJ\u0010\u0010J\u001a\u00020B2\u0006\u0010:\u001a\u00020\u0016H\u0002J\u0012\u0010K\u001a\u0004\u0018\u00010\u00072\u0006\u0010L\u001a\u00020\u0007H\u0002J\u0012\u0010M\u001a\u0004\u0018\u00010\u00072\u0006\u0010L\u001a\u00020\u0007H\u0002J\u0012\u0010N\u001a\u0004\u0018\u00010\u00072\u0006\u0010L\u001a\u00020\u0007H\u0002J\u0012\u0010O\u001a\u0004\u0018\u00010\u00072\u0006\u0010L\u001a\u00020\u0007H\u0002J\u0010\u0010P\u001a\u00020$2\u0006\u0010L\u001a\u00020\u0007H\u0002J\u0010\u0010Q\u001a\u00020B2\u0006\u0010A\u001a\u00020BH\u0002J\u000e\u0010R\u001a\u00020\u00072\u0006\u0010\u0018\u001a\u00020\u0007J\u000e\u0010S\u001a\u0002032\u0006\u0010\u0018\u001a\u00020\u0007J\u0010\u0010T\u001a\u0004\u0018\u00010U2\u0006\u0010\u0018\u001a\u00020\u0007J\u0010\u0010V\u001a\u00020U2\u0006\u0010\u0018\u001a\u00020\u0007H\u0002J\u0014\u0010W\u001a\b\u0012\u0004\u0012\u00020Y0XH\u0086@\u00a2\u0006\u0002\u0010ZJ\u001e\u0010[\u001a\b\u0012\u0004\u0012\u0002030\u00152\u0006\u0010\u0018\u001a\u00020\u0007H\u0086@\u00a2\u0006\u0004\b\\\u0010]J\u001e\u0010^\u001a\b\u0012\u0004\u0012\u00020Y0\u00152\u0006\u0010\u0018\u001a\u00020\u0007H\u0086@\u00a2\u0006\u0004\b_\u0010]J\u001e\u0010`\u001a\b\u0012\u0004\u0012\u0002030\u00152\u0006\u0010\u0018\u001a\u00020\u0007H\u0086@\u00a2\u0006\u0004\ba\u0010]J\b\u0010b\u001a\u00020\u0016H\u0002J\u000e\u0010c\u001a\b\u0012\u0004\u0012\u00020e0dH\u0002J\u000e\u0010f\u001a\b\u0012\u0004\u0012\u00020e0dH\u0002J\u0016\u0010g\u001a\u0002032\f\u0010h\u001a\b\u0012\u0004\u0012\u00020e0XH\u0002J\u000e\u0010i\u001a\b\u0012\u0004\u0012\u00020e0XH\u0002J\u0010\u0010j\u001a\u00020\u00072\u0006\u0010\u0018\u001a\u00020\u0007H\u0002J\u0010\u0010k\u001a\u00020Y2\u0006\u0010l\u001a\u00020eH\u0002J\u0010\u0010m\u001a\u00020\u00072\u0006\u0010n\u001a\u00020oH\u0002R\u000e\u0010\u0004\u001a\u00020\u0005X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0006\u001a\u00020\u0007X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\b\u001a\u00020\u0007X\u0082T\u00a2\u0006\u0002\n\u0000R\u0016\u0010\t\u001a\n \u000b*\u0004\u0018\u00010\n0\nX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\f\u001a\u00020\rX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u000e\u001a\u00020\rX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0014\u0010\u000f\u001a\b\u0012\u0004\u0012\u00020\u00070\u0010X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0014\u0010\u0011\u001a\b\u0012\u0004\u0012\u00020\u00070\u0010X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0014\u0010\u0012\u001a\b\u0012\u0004\u0012\u00020\u00070\u0010X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0013\u001a\u00020\u0007X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006p"}, d2 = {"Lcom/jmwl/gostudio/project/project_manager;", "", "<init>", "()V", "max_recent_projects", "", "project_config_dir_name", "", "project_config_file_name", "json", "Lcom/google/gson/Gson;", "kotlin.jvm.PlatformType", "valid_project_name", "Lkotlin/text/Regex;", "valid_android_platform", "supported_cpp_standards", "", "supported_build_abis", "supported_build_types", "default_clang_format", "create_project", "Lkotlin/Result;", "Ljava/io/File;", "name", "path", "template_id", "ndk_version", "cmake_version", "android_platform", "cpp_standard", "create_project-eH_QyT8", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "create_project_entry", "project_path", "parent_path", "directory", "", "create_project_entry-yxL6bBk", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;ZLkotlin/coroutines/Continuation;)Ljava/lang/Object;", "rename_project_entry", "Lkotlin/Pair;", "new_name", "rename_project_entry-BWLJW6A", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "resolve_project_entry_for_delete", "resolve_project_entry_for_delete-0E7RQCE", "(Ljava/lang/String;Ljava/lang/String;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "delete_project_entry", "delete_project_entry-0E7RQCE", "normalize_project_entry_name", "create_go_project", "", "dir", "go_hello_template", "go_http_template", "go_cli_template", "go_webapi_template", "project_config_file", "project_dir", "ensure_project_clang_format", "ensure_project_clang_format-IoAF18A", "(Ljava/lang/String;)Ljava/lang/Object;", "ensure_project_config", "ensure_project_config-IoAF18A", "write_project_config", "build", "Lcom/jmwl/gostudio/project/project_build_config;", "read_project_build_config", "read_project_ide_config", "Lcom/jmwl/gostudio/project/project_ide_config;", "save_project_ide_config", "ide_config", "save_project_ide_config-gIAlu-s", "(Ljava/lang/String;Lcom/jmwl/gostudio/project/project_ide_config;)Ljava/lang/Object;", "infer_imported_project_build_config", "infer_cmake_android_abi", "content", "infer_cmake_android_platform", "infer_cmake_cpp_standard", "infer_cmake_build_type", "uses_android_vulkan", "normalize_project_build_config", "get_project_last_opened", "update_project_opened_time", "get_project_info", "Lcom/jmwl/gostudio/project/project_info;", "require_xcode_project_info", "get_recent_projects", "", "Lcom/jmwl/gostudio/project/recent_project_info;", "(Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "check_project_toolchain", "check_project_toolchain-gIAlu-s", "(Ljava/lang/String;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "add_recent_project", "add_recent_project-gIAlu-s", "remove_recent_project", "remove_recent_project-gIAlu-s", "recent_projects_file", "load_recent_project_records", "", "Lcom/jmwl/gostudio/project/recent_project_record;", "read_recent_project_records", "write_recent_project_records", "records", "discover_project_records", "normalize_project_path", "create_recent_project_info", "record", "format_last_opened", "opened_at", "", "app_release"})
public final class project_manager {
    private static final int max_recent_projects = 20;
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String project_config_dir_name = ".xcode";
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String project_config_file_name = ".xcode-project.json";
    private static final com.google.gson.Gson json = null;
    @org.jetbrains.annotations.NotNull()
    private static final kotlin.text.Regex valid_project_name = null;
    @org.jetbrains.annotations.NotNull()
    private static final kotlin.text.Regex valid_android_platform = null;
    @org.jetbrains.annotations.NotNull()
    private static final java.util.Set<java.lang.String> supported_cpp_standards = null;
    @org.jetbrains.annotations.NotNull()
    private static final java.util.Set<java.lang.String> supported_build_abis = null;
    @org.jetbrains.annotations.NotNull()
    private static final java.util.Set<java.lang.String> supported_build_types = null;
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String default_clang_format = null;
    @org.jetbrains.annotations.NotNull()
    public static final com.jmwl.gostudio.project.project_manager INSTANCE = null;
    
    private project_manager() {
        super();
    }
    
    private final java.lang.String normalize_project_entry_name(java.lang.String name) {
        return null;
    }
    
    /**
     * 创建 Go 项目文件（go.mod + main.go）。
     *
     * @param template_id 模板：hello(默认)/http/cli/webapi
     * Go 项目不需要 CMakeLists/ndk/cmake，仅 go.mod + main.go 即可 go run。
     */
    private final void create_go_project(java.io.File dir, java.lang.String name, java.lang.String template_id) {
    }
    
    /**
     * Hello World 模板
     */
    private final java.lang.String go_hello_template(java.lang.String name) {
        return null;
    }
    
    /**
     * HTTP 服务器模板（net/http）
     */
    private final java.lang.String go_http_template(java.lang.String name) {
        return null;
    }
    
    /**
     * CLI 工具模板（os.Args）
     */
    private final java.lang.String go_cli_template(java.lang.String name) {
        return null;
    }
    
    /**
     * Web API 模板（标准库 JSON API）
     */
    private final java.lang.String go_webapi_template(java.lang.String name) {
        return null;
    }
    
    private final java.io.File project_config_file(java.io.File project_dir) {
        return null;
    }
    
    private final void write_project_config(java.io.File dir, java.lang.String name, java.lang.String ndk_version, java.lang.String cmake_version, java.lang.String template_id, com.jmwl.gostudio.project.project_build_config build) {
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.jmwl.gostudio.project.project_build_config read_project_build_config(@org.jetbrains.annotations.NotNull()
    java.lang.String path) {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.jmwl.gostudio.project.project_ide_config read_project_ide_config(@org.jetbrains.annotations.NotNull()
    java.lang.String path) {
        return null;
    }
    
    private final com.jmwl.gostudio.project.project_build_config infer_imported_project_build_config(java.io.File project_dir) {
        return null;
    }
    
    private final java.lang.String infer_cmake_android_abi(java.lang.String content) {
        return null;
    }
    
    private final java.lang.String infer_cmake_android_platform(java.lang.String content) {
        return null;
    }
    
    private final java.lang.String infer_cmake_cpp_standard(java.lang.String content) {
        return null;
    }
    
    private final java.lang.String infer_cmake_build_type(java.lang.String content) {
        return null;
    }
    
    private final boolean uses_android_vulkan(java.lang.String content) {
        return false;
    }
    
    private final com.jmwl.gostudio.project.project_build_config normalize_project_build_config(com.jmwl.gostudio.project.project_build_config build) {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String get_project_last_opened(@org.jetbrains.annotations.NotNull()
    java.lang.String path) {
        return null;
    }
    
    public final void update_project_opened_time(@org.jetbrains.annotations.NotNull()
    java.lang.String path) {
    }
    
    @org.jetbrains.annotations.Nullable()
    public final com.jmwl.gostudio.project.project_info get_project_info(@org.jetbrains.annotations.NotNull()
    java.lang.String path) {
        return null;
    }
    
    private final com.jmwl.gostudio.project.project_info require_xcode_project_info(java.lang.String path) {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object get_recent_projects(@org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.util.List<com.jmwl.gostudio.project.recent_project_info>> $completion) {
        return null;
    }
    
    private final java.io.File recent_projects_file() {
        return null;
    }
    
    private final java.util.List<com.jmwl.gostudio.project.recent_project_record> load_recent_project_records() {
        return null;
    }
    
    private final java.util.List<com.jmwl.gostudio.project.recent_project_record> read_recent_project_records() {
        return null;
    }
    
    private final void write_recent_project_records(java.util.List<com.jmwl.gostudio.project.recent_project_record> records) {
    }
    
    private final java.util.List<com.jmwl.gostudio.project.recent_project_record> discover_project_records() {
        return null;
    }
    
    private final java.lang.String normalize_project_path(java.lang.String path) {
        return null;
    }
    
    private final com.jmwl.gostudio.project.recent_project_info create_recent_project_info(com.jmwl.gostudio.project.recent_project_record record) {
        return null;
    }
    
    private final java.lang.String format_last_opened(long opened_at) {
        return null;
    }
}