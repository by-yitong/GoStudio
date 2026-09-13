# ===== GoStudio R8 keep 规则 =====
# 思路：自有代码整体保留（gson/JSON 反射模型密集：编辑器/AI/项目/插件配置，
# 逐个排查成本高且易漏），裁剪收益来自三方库（compose/icons 等未用代码）。

# 自有代码（含全部 com.jmwl.gostudio.* 模块）：不混淆不裁剪
-keep class com.jmwl.gostudio.** { *; }

# 运行时 APK 打包依赖 apksig：运行时直接调用且内部有反射，整体保留
-keep class com.android.apksig.** { *; }

# sora-editor / textmate 高亮栈：语言工厂按类名注册，oniguruma JNI 按名绑定
-keep class io.github.rosemoe.** { *; }
-keep class org.eclipse.tm4e.** { *; }

# termux terminal-view
-keep class com.termux.** { *; }

# gson / retrofit 泛型签名（TypeToken 解析需要 Signature 属性）
-keepattributes Signature, InnerClasses, EnclosingMethod, *Annotation*, AnnotationDefault

# 枚举反射（valueOf / values）
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# 崩溃堆栈保留行号
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Kotlin Cloneable 默认实现：sora 旧代码引用，运行时不存在（Kotlin 编译器内联），忽略
-dontwarn kotlin.Cloneable$DefaultImpls
