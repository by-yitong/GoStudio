package com.jmwl.gostudio.runtime

import android.content.Context
import com.android.apksig.ApkSigner
import java.io.File
import java.io.FileOutputStream
import java.security.KeyStore
import java.security.PrivateKey
import java.security.cert.Certificate
import java.security.cert.X509Certificate
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

/**
 * 把「layout.xml + Go 二进制」打进壳模板并签名，产出可安装的独立 APK。
 *
 * 流程：
 * 1. 从 assets 复制壳模板（未签名）到临时目录
 * 2. 重写 ZIP：保留原条目（跳过 META-INF 旧签名），追加 assets/app/layout.xml 与 assets/app/app.bin
 * 3. apksig v1+v2 签名（内置 pack.keystore）
 */
object apk_packer {

    fun pack(
        context: Context,
        layout_file: File,
        binary_file: File,
        output_apk: File,
        app_name: String = "App",
        package_name: String = "",
        version_name: String = "",
        icon_file: File? = null
    ): Result<File> = runCatching {
        val work_dir = File(context.cacheDir, "apkpack").apply { mkdirs() }

        // 1. 复制模板
        val template = File(work_dir, "template.apk")
        context.assets.open("apkpack/shell-template.apk").use { input ->
            FileOutputStream(template).use { input.copyTo(it) }
        }

        // 2. 注入
        val injected = File(work_dir, "injected.apk")
        inject(template, injected, layout_file, binary_file)

        // 3. 改写 Manifest（名称/包名/版本）
        val rewritten = File(work_dir, "rewritten.apk")
        rewrite_manifest(injected, rewritten, app_name, package_name, version_name)

        // 4. 替换图标
        val with_icon = if (icon_file != null && icon_file.isFile) {
            val iconed = File(work_dir, "iconed.apk")
            replace_icons(rewritten, iconed, icon_file)
            iconed
        } else rewritten

        // 5. 签名（直接写 output_apk）
        output_apk.parentFile?.mkdirs()
        sign(context, with_icon, output_apk)
        output_apk
    }

    /** 重写 ZIP：复制原条目（跳过旧签名），再追加两个 assets 条目。 */
    private fun inject(template: File, output: File, layout_file: File, binary_file: File) {
        ZipFile(template).use { zf ->
            ZipOutputStream(FileOutputStream(output)).use { zos ->
                val entries = zf.entries()
                while (entries.hasMoreElements()) {
                    val entry = entries.nextElement()
                    if (entry.name.startsWith("META-INF/")) continue
                    // 保留原压缩方式与时间戳
                    val copy = ZipEntry(entry.name).apply {
                        time = entry.time
                        if (entry.method == ZipEntry.STORED) {
                            // STORED 条目必须提供 size/crc；DEFLATED 条目交给 ZipOutputStream
                            // 重新压缩计算，预设 compressedSize 会因字节数不一致而报错。
                            method = ZipEntry.STORED
                            size = entry.size
                            crc = entry.crc
                        }
                    }
                    zos.putNextEntry(copy)
                    zf.getInputStream(entry).copyTo(zos)
                    zos.closeEntry()
                }
                add_entry(zos, "assets/app/layout.xml", layout_file)
                add_entry(zos, "assets/app/app.bin", binary_file)
            }
        }
    }

    private fun add_entry(zos: ZipOutputStream, name: String, file: File) {
        zos.putNextEntry(ZipEntry(name).apply { time = FIXED_TIME })
        file.inputStream().use { it.copyTo(zos) }
        zos.closeEntry()
    }

    /** 重写 AndroidManifest.xml 中的 label/package/versionName 字符串。 */
    private fun rewrite_manifest(input: File, output: File, app_name: String, package_name: String, version_name: String) {
        // 先解出 manifest，改字符串池，再整包重写
        val rewritten_apk = File(output.parentFile, "manifest-only.apk")
        ZipFile(input).use { zf ->
            val manifest_entry = zf.getEntry("AndroidManifest.xml")
            val manifest_bytes = zf.getInputStream(manifest_entry).readBytes()
            val replacements = buildMap {
                put("App", app_name)
                if (package_name.isNotBlank()) put("com.jmwl.gostudio.shell", package_name)
                if (version_name.isNotBlank()) put("1.0", version_name)
            }
            val new_manifest = binary_manifest_editor.replace_strings(manifest_bytes, replacements)

            ZipOutputStream(FileOutputStream(rewritten_apk)).use { zos ->
                val entries = zf.entries()
                while (entries.hasMoreElements()) {
                    val entry = entries.nextElement()
                    val copy = ZipEntry(entry.name).apply {
                        time = entry.time
                        if (entry.method == ZipEntry.STORED) {
                            method = ZipEntry.STORED
                            size = if (entry.name == "AndroidManifest.xml") new_manifest.size.toLong() else entry.size
                            crc = if (entry.name == "AndroidManifest.xml") java.util.zip.CRC32().apply { update(new_manifest) }.value else entry.crc
                        }
                    }
                    zos.putNextEntry(copy)
                    if (entry.name == "AndroidManifest.xml") zos.write(new_manifest)
                    else zf.getInputStream(entry).copyTo(zos)
                    zos.closeEntry()
                }
            }
        }
        rewritten_apk.copyTo(output, overwrite = true)
    }

    /** 用同一张 PNG 替换壳内全部启动图标（ic_launcher / ic_launcher_round）。 */
    private fun replace_icons(input: File, output: File, icon: File) {
        val icon_bytes = icon.readBytes()
        val icon_crc = java.util.zip.CRC32().apply { update(icon_bytes) }
        ZipFile(input).use { zf ->
            ZipOutputStream(FileOutputStream(output)).use { zos ->
                val entries = zf.entries()
                while (entries.hasMoreElements()) {
                    val entry = entries.nextElement()
                    val is_launcher_icon = entry.name.startsWith("res/mipmap-") &&
                        (entry.name.endsWith("ic_launcher.png") || entry.name.endsWith("ic_launcher_round.png"))
                    val copy = ZipEntry(entry.name).apply {
                        time = FIXED_TIME
                        if (entry.method == ZipEntry.STORED) {
                            method = ZipEntry.STORED
                            size = if (is_launcher_icon) icon_bytes.size.toLong() else entry.size
                            crc = if (is_launcher_icon) icon_crc.value else entry.crc
                        }
                    }
                    zos.putNextEntry(copy)
                    if (is_launcher_icon) zos.write(icon_bytes)
                    else zf.getInputStream(entry).copyTo(zos)
                    zos.closeEntry()
                }
            }
        }
    }

    /** v1+v2 签名，输出到目标文件。 */
    private fun sign(context: Context, input: File, output: File) {
        val key_store = KeyStore.getInstance("PKCS12")
        context.assets.open("apkpack/pack.keystore").use { input_stream ->
            key_store.load(input_stream, KEYSTORE_PASSWORD.toCharArray())
        }
        val entry = key_store.getEntry(KEY_ALIAS, KeyStore.PasswordProtection(KEYSTORE_PASSWORD.toCharArray()))
        val private_key = (entry as KeyStore.PrivateKeyEntry).privateKey
        val certificate = entry.certificate as X509Certificate

        val signer_config = ApkSigner.SignerConfig.Builder(
            "CERT", private_key as PrivateKey, listOf<X509Certificate>(certificate)
        ).build()
        ApkSigner.Builder(listOf(signer_config))
            .setV1SigningEnabled(true)
            .setV2SigningEnabled(true)
            .setV3SigningEnabled(false)
            .setInputApk(input)
            .setOutputApk(output)
            .build()
            .sign()
    }

    private const val FIXED_TIME = 315532800000L // 2000-01-01，保证构建可重复
    private const val KEYSTORE_PASSWORD = "gostudio"
    private const val KEY_ALIAS = "pack"
}
