package com.jmwl.gostudio.project;

@kotlin.Metadata(mv = {2, 3, 0}, k = 1, xi = 48, d1 = {"\u0000\u001c\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0003\n\u0002\u0010\u000e\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0002\b\u00c6\u0002\u0018\u00002\u00020\u0001B\t\b\u0002\u00a2\u0006\u0004\b\u0002\u0010\u0003J\u000e\u0010\b\u001a\u00020\t2\u0006\u0010\n\u001a\u00020\u0005R\u000e\u0010\u0004\u001a\u00020\u0005X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0006\u001a\u00020\u0005X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0007\u001a\u00020\u0005X\u0082T\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u000b"}, d2 = {"Lcom/jmwl/gostudio/project/project_detector;", "", "<init>", "()V", "cmake_file_name", "", "default_cmake_build_dir", "compile_commands_file_name", "detect_project", "Lcom/jmwl/gostudio/project/detected_project;", "project_path", "app_release"})
public final class project_detector {
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String cmake_file_name = "CMakeLists.txt";
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String default_cmake_build_dir = "build";
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String compile_commands_file_name = "compile_commands.json";
    @org.jetbrains.annotations.NotNull()
    public static final com.jmwl.gostudio.project.project_detector INSTANCE = null;
    
    private project_detector() {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.jmwl.gostudio.project.detected_project detect_project(@org.jetbrains.annotations.NotNull()
    java.lang.String project_path) {
        return null;
    }
}