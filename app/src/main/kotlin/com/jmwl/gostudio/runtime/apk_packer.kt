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
        output_apk: File
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

        // 3. 签名（直接写 output_apk）
        output_apk.parentFile?.mkdirs()
        sign(context, injected, output_apk)
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
