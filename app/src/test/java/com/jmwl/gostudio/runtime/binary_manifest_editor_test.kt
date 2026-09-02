package com.jmwl.gostudio.runtime

import com.android.apksig.apk.ApkUtils
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.zip.ZipFile

/**
 * 用真实壳模板（assets/apkpack/shell-template.apk）的 AndroidManifest.xml 做回归：
 * 改写后的二进制 manifest 必须仍能被 apksig 解析——打包签名时
 * ApkSigner 正是靠它推断 minSdkVersion，解析失败即“打包失败”。
 */
class binary_manifest_editor_test {

    private fun template_manifest(): ByteArray {
        val apk = File("src/main/assets/apkpack/shell-template.apk")
        ZipFile(apk).use { zip ->
            return zip.getInputStream(zip.getEntry("AndroidManifest.xml")).readBytes()
        }
    }

    private fun little_endian(bytes: ByteArray) =
        ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)

    @Test
    fun `rewritten manifest stays parseable by apksig`() {
        val rewritten = binary_manifest_editor.replace_strings(
            template_manifest(),
            mapOf(
                "App" to "我的应用",
                "com.jmwl.gostudio.shell" to "com.example.packed",
                "1.0" to "2.3"
            )
        )

        // 与 apk_packer.sign() 相同的解析路径：结构改坏会在这里抛 MinSdkVersionException
        val min_sdk = ApkUtils.getMinSdkVersionFromBinaryAndroidManifest(little_endian(rewritten))
        assertThat(min_sdk).isGreaterThan(0)

        assertThat(
            ApkUtils.getPackageNameFromBinaryAndroidManifest(little_endian(rewritten))
        ).isEqualTo("com.example.packed")

        assertThat(
            ApkUtils.getVersionCodeFromBinaryAndroidManifest(little_endian(rewritten))
        ).isGreaterThan(0)
    }

    @Test
    fun `rewritten manifest keeps a consistent chunk structure`() {
        val rewritten = binary_manifest_editor.replace_strings(
            template_manifest(),
            mapOf("App" to "一个很长很长很长很长很长很长的应用名称用于验证池重建")
        )

        val buffer = little_endian(rewritten)
        assertThat(buffer.short.toInt() and 0xFFFF).isEqualTo(0x0003)
        assertThat(buffer.short.toInt() and 0xFFFF).isEqualTo(8) // headerSize
        assertThat(buffer.int).isEqualTo(rewritten.size)

        // 文件头后紧跟字符串池
        buffer.position(8)
        assertThat(buffer.short.toInt() and 0xFFFF).isEqualTo(0x0001)
        val pool_size = buffer.int
        assertThat(pool_size % 4).isEqualTo(0)

        // 逐 chunk 步进必须正好落在文件末尾（任一 size 字段错误都会失步）
        var offset = 8
        while (offset < rewritten.size) {
            buffer.position(offset)
            buffer.short // type
            buffer.short // headerSize
            val size = buffer.int
            assertThat(size).isAtLeast(8)
            offset += size
        }
        assertThat(offset).isEqualTo(rewritten.size)
    }

    @Test
    fun `no matching replacements returns original bytes`() {
        val manifest = template_manifest()
        val result = binary_manifest_editor.replace_strings(
            manifest,
            mapOf("不存在的字符串" to "x")
        )
        assertThat(result).isEqualTo(manifest)
    }

    @Test
    fun `package prefix swap rewrites authorities and permissions but keeps shell class`() {
        val rewritten = binary_manifest_editor.replace_strings(
            template_manifest(),
            replacements = mapOf("com.jmwl.gostudio.shell" to "com.example.packed"),
            prefix_replacements = mapOf("com.jmwl.gostudio.shell" to "com.example.packed"),
            protected_strings = setOf("com.jmwl.gostudio.shell.shell_activity")
        )

        val strings = pool_strings(rewritten)
        // provider 授权与动态权限声明要跟着换包名，否则两个项目的 APK 无法共存
        assertThat(strings).contains("com.example.packed.androidx-startup")
        assertThat(strings).contains("com.example.packed.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION")
        // 壳 Activity 类名必须保留：它指向 DEX 里的真实类
        assertThat(strings).contains("com.jmwl.gostudio.shell.shell_activity")
        assertThat(strings).doesNotContain("com.jmwl.gostudio.shell.androidx-startup")

        assertThat(
            ApkUtils.getPackageNameFromBinaryAndroidManifest(little_endian(rewritten))
        ).isEqualTo("com.example.packed")
    }

    @Test
    fun `set_version_code patches the manifest versionCode in place`() {
        val rewritten = binary_manifest_editor.replace_strings(
            template_manifest(),
            replacements = mapOf("com.jmwl.gostudio.shell" to "com.example.packed"),
            prefix_replacements = mapOf("com.jmwl.gostudio.shell" to "com.example.packed"),
            protected_strings = setOf("com.jmwl.gostudio.shell.shell_activity")
        )
        val patched = binary_manifest_editor.set_version_code(rewritten, 107)

        assertThat(
            ApkUtils.getVersionCodeFromBinaryAndroidManifest(little_endian(patched))
        ).isEqualTo(107)
        assertThat(
            ApkUtils.getPackageNameFromBinaryAndroidManifest(little_endian(patched))
        ).isEqualTo("com.example.packed")

        // versionCode 是定长整型属性，改写不应改变文件长度
        assertThat(patched.size).isEqualTo(rewritten.size)

        // 结构仍需可步进
        val buffer = little_endian(patched)
        var offset = 8
        while (offset < patched.size) {
            buffer.position(offset)
            buffer.short
            buffer.short
            val size = buffer.int
            assertThat(size).isAtLeast(8)
            offset += size
        }
        assertThat(offset).isEqualTo(patched.size)
    }

    /** 解析 AXML 字符串池为字符串列表（与 binary_manifest_editor 相同的解析规则）。 */
    private fun pool_strings(manifest: ByteArray): List<String> {
        val buffer = little_endian(manifest)
        buffer.position(8)
        assertThat(buffer.short.toInt() and 0xFFFF).isEqualTo(0x0001)
        buffer.short // headerSize
        buffer.int // chunk size
        val string_count = buffer.int
        buffer.int // styleCount
        val flags = buffer.int
        val strings_start = buffer.int
        buffer.int // stylesStart
        val is_utf8 = (flags and (1 shl 8)) != 0
        val offsets = IntArray(string_count) { buffer.int }

        val strings = mutableListOf<String>()
        for (offset in offsets) {
            val pos = 8 + strings_start + offset
            var p = pos
            val text: String = if (is_utf8) {
                val u16len = read_len8(manifest, p).also { p += it.second }.first
                val u8len = read_len8(manifest, p).also { p += it.second }.first
                String(manifest, p, u8len, Charsets.UTF_8)
            } else {
                val len = read_len16(manifest, p).also { p += it.second }.first
                String(manifest, p, len * 2, Charsets.UTF_16LE)
            }
            strings.add(text)
        }
        return strings
    }

    private fun read_len8(data: ByteArray, pos: Int): Pair<Int, Int> {
        val first = data[pos].toInt() and 0xFF
        return if (first and 0x80 != 0) {
            ((first and 0x7F) shl 8) or (data[pos + 1].toInt() and 0xFF) to 2
        } else first to 1
    }

    private fun read_len16(data: ByteArray, pos: Int): Pair<Int, Int> {
        val first = (data[pos].toInt() and 0xFF) or ((data[pos + 1].toInt() and 0xFF) shl 8)
        return if (first and 0x8000 != 0) {
            ((first and 0x7FFF) shl 16) or (
                (data[pos + 2].toInt() and 0xFF) or ((data[pos + 3].toInt() and 0xFF) shl 8)
                ) to 4
        } else first to 2
    }
}
