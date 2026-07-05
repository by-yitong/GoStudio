package com.xc.code.toolchain.runtime

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.EOFException
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.nio.file.Files
import java.security.MessageDigest
import java.util.Locale
import java.util.zip.GZIPInputStream
import org.tukaani.xz.XZInputStream

sealed class rootfs_install_event {
    data class log(val message: String) : rootfs_install_event()
    data class download(
        val percent: Int,
        val downloaded_size: Long,
        val total_size: Long,
        val speed: Long
    ) : rootfs_install_event()
    data class extract(
        val percent: Int,
        val current_file: String
    ) : rootfs_install_event()
}

class rootfs_installer(
    private val paths: toolchain_runtime_paths
) {
    suspend fun install_ubuntu_base(
        mirrors: List<String>,
        file_name: String,
        expected_md5: String,
        on_event: suspend (rootfs_install_event) -> Unit
    ): Boolean = withContext(Dispatchers.IO) {
        require(mirrors.isNotEmpty()) { "Rootfs mirrors are required" }
        val cache_dir = File(paths.xcode_dir, "cache").apply { mkdirs() }
        val archive = File(cache_dir, file_name)

        if (archive.exists() && archive.length() > 0) {
            on_event(rootfs_install_event.log("找到缓存文件"))
            on_event(rootfs_install_event.log("验证 MD5..."))
            if (verify_md5(archive, expected_md5, on_event)) {
                on_event(rootfs_install_event.log("MD5 校验通过"))
                return@withContext extract_rootfs(archive, on_event)
            }
            on_event(rootfs_install_event.log("缓存文件 MD5 校验失败，删除缓存文件"))
            archive.delete()
        }

        var download_ok = false
        for ((index, mirror) in mirrors.withIndex()) {
            if (archive.exists()) archive.delete()
            on_event(rootfs_install_event.log("尝试镜像 ${index + 1}/${mirrors.size}"))
            if (!download(mirror, archive, on_event)) {
                on_event(rootfs_install_event.log("下载失败"))
                archive.delete()
                continue
            }
            on_event(rootfs_install_event.log("验证 MD5..."))
            if (verify_md5(archive, expected_md5, on_event)) {
                download_ok = true
                on_event(rootfs_install_event.log("下载完成"))
                break
            }
            on_event(rootfs_install_event.log("MD5 校验失败，文件可能损坏"))
            archive.delete()
        }

        if (!download_ok) {
            on_event(rootfs_install_event.log("所有镜像均下载失败"))
            on_event(rootfs_install_event.log("请检查网络连接后重试"))
            return@withContext false
        }

        extract_rootfs(archive, on_event)
    }

    private suspend fun download(
        url: String,
        output_file: File,
        on_event: suspend (rootfs_install_event) -> Unit
    ): Boolean {
        var connection: HttpURLConnection? = null
        var input: InputStream? = null
        var output: FileOutputStream? = null
        val temp_file = File(output_file.parentFile, "${output_file.name}.download")

        return try {
            output_file.parentFile?.mkdirs()
            if (temp_file.exists()) temp_file.delete()

            connection = URL(url).openConnection() as HttpURLConnection
            connection.connectTimeout = connect_timeout_ms
            connection.readTimeout = read_timeout_ms
            connection.instanceFollowRedirects = true
            connection.requestMethod = "GET"
            connection.setRequestProperty("User-Agent", "XCode/1.0")
            connection.connect()
            if (connection.responseCode !in 200..299) return false

            val total_size = connection.contentLengthLong.coerceAtLeast(0)
            input = connection.inputStream
            output = FileOutputStream(temp_file)

            val buffer = ByteArray(buffer_size)
            var downloaded_size = 0L
            var last_bytes = 0L
            var last_update_time = System.currentTimeMillis()
            while (true) {
                check_interrupted()
                val read = input.read(buffer)
                if (read == -1) break
                output.write(buffer, 0, read)
                downloaded_size += read
                last_bytes += read

                val now = System.currentTimeMillis()
                val elapsed = now - last_update_time
                if (elapsed >= 200) {
                    val speed = if (elapsed > 0) last_bytes * 1000 / elapsed else 0
                    val percent = if (total_size > 0) ((downloaded_size * 100) / total_size).toInt() else 0
                    on_event(rootfs_install_event.download(percent, downloaded_size, total_size, speed))
                    last_update_time = now
                    last_bytes = 0
                }
            }

            output.flush()
            output.close()
            output = null
            if (output_file.exists()) output_file.delete()
            if (!temp_file.renameTo(output_file)) {
                temp_file.copyTo(output_file, overwrite = true)
                temp_file.delete()
            }
            on_event(rootfs_install_event.download(100, downloaded_size, total_size, 0))
            true
        } catch (_: Exception) {
            false
        } finally {
            connection?.disconnect()
            input?.close()
            output?.close()
        }
    }

    private suspend fun verify_md5(
        file: File,
        expected_md5: String,
        on_event: suspend (rootfs_install_event) -> Unit
    ): Boolean {
        val calculated = calculate_md5(file)
        on_event(rootfs_install_event.log("计算 MD5: $calculated"))
        return calculated == expected_md5
    }

    private fun calculate_md5(file: File): String? {
        if (!file.exists()) return null
        return try {
            val digest = MessageDigest.getInstance("MD5")
            val buffer = ByteArray(8192)
            FileInputStream(file).use { input ->
                while (true) {
                    val read = input.read(buffer)
                    if (read == -1) break
                    digest.update(buffer, 0, read)
                }
            }
            digest.digest().joinToString("") { "%02x".format(it) }
        } catch (_: Exception) {
            null
        }
    }

    private suspend fun extract_rootfs(
        archive: File,
        on_event: suspend (rootfs_install_event) -> Unit
    ): Boolean {
        on_event(rootfs_install_event.log("开始解压 Ubuntu 文件系统..."))
        val output_dir = paths.ubuntu_base_dir
        val staging_dir = File(paths.proot_tmp_dir, "ubuntu-base-staging")
        staging_dir.deleteRecursively()
        staging_dir.mkdirs()

        return try {
            val total_entries = count_tar_entries(archive).coerceAtLeast(1)
            extract_tar(archive, staging_dir, total_entries, on_event)
            rootfs_patcher().patch(paths.copy(ubuntu_base_dir = staging_dir))
            if (output_dir.exists()) {
                on_event(rootfs_install_event.log("清理旧目录"))
                output_dir.deleteRecursively()
            }
            require(staging_dir.renameTo(output_dir)) { "Failed to move Ubuntu rootfs into place" }
            on_event(rootfs_install_event.log("Ubuntu 解压完成"))
            true
        } catch (_: Exception) {
            on_event(rootfs_install_event.log("Ubuntu 解压失败"))
            staging_dir.deleteRecursively()
            on_event(rootfs_install_event.log("请重试"))
            false
        }
    }

    private fun count_tar_entries(archive: File): Int {
        archive_format.from_file(archive).wrap_stream(BufferedInputStream(archive.inputStream())).use { input ->
            var entries = 0
            while (true) {
                check_interrupted()
                val header = input.read_tar_header() ?: break
                input.skip_fully(header.size.padded_tar_size())
                if (header.name.isNotBlank() && header.type !in listOf(tar_entry_type.LONG_NAME, tar_entry_type.LONG_LINK, tar_entry_type.PAX)) {
                    entries++
                }
            }
            return entries
        }
    }

    private suspend fun extract_tar(
        archive: File,
        target_dir: File,
        total_entries: Int,
        on_event: suspend (rootfs_install_event) -> Unit
    ) {
        archive_format.from_file(archive).wrap_stream(BufferedInputStream(archive.inputStream())).use { input ->
            var entries = 0
            var pending_name: String? = null
            var pending_link_name: String? = null
            while (true) {
                check_interrupted()
                val raw_header = input.read_tar_header() ?: break
                val header = raw_header.copy(
                    name = pending_name ?: raw_header.name,
                    link_name = pending_link_name ?: raw_header.link_name
                )
                pending_name = null
                pending_link_name = null

                if (header.name.isBlank()) {
                    input.skip_fully(header.size.padded_tar_size())
                    continue
                }
                if (header.type == tar_entry_type.LONG_NAME) {
                    pending_name = input.read_exactly(header.size).toString(Charsets.UTF_8).trimEnd('\u0000', '\n')
                    input.skip_fully(header.size.padding_size())
                    continue
                }
                if (header.type == tar_entry_type.LONG_LINK) {
                    pending_link_name = input.read_exactly(header.size).toString(Charsets.UTF_8).trimEnd('\u0000', '\n')
                    input.skip_fully(header.size.padding_size())
                    continue
                }
                if (header.type == tar_entry_type.PAX) {
                    val pax = parse_pax(input.read_exactly(header.size).toString(Charsets.UTF_8))
                    pending_name = pax["path"]
                    pending_link_name = pax["linkpath"]
                    input.skip_fully(header.size.padding_size())
                    continue
                }

                val target = target_dir.safe_resolve(header.name)
                target.parentFile?.mkdirs()
                when (header.type) {
                    tar_entry_type.DIRECTORY -> target.mkdirs()
                    tar_entry_type.SYMLINK -> create_symlink(target_dir, target, header.link_name)
                    tar_entry_type.HARDLINK -> create_hard_link(target_dir, target, header.link_name)
                    tar_entry_type.FILE -> {
                        target.outputStream().use { output -> input.copy_exactly(output, header.size) }
                        target.apply_mode(header.mode)
                    }
                    tar_entry_type.LONG_NAME,
                    tar_entry_type.LONG_LINK,
                    tar_entry_type.PAX,
                    tar_entry_type.OTHER -> Unit
                }
                if (header.type != tar_entry_type.FILE) input.skip_fully(header.size)
                input.skip_fully(header.size.padding_size())
                if (header.mod_time > 0 && header.type != tar_entry_type.SYMLINK) {
                    target.setLastModified(header.mod_time * 1000)
                }
                entries++
                val percent = ((entries * 100L) / total_entries).toInt().coerceIn(0, 100)
                on_event(rootfs_install_event.extract(percent, header.name))
            }
            on_event(rootfs_install_event.extract(100, ""))
        }
    }

    private fun create_symlink(root: File, target: File, link_name: String) {
        if (link_name.isBlank()) return
        val link_target = if (File(link_name).isAbsolute) {
            File(link_name)
        } else {
            val resolved = File(target.parentFile ?: root, link_name).canonicalFile
            val root_file = root.canonicalFile
            require(resolved.path == root_file.path || resolved.path.startsWith(root_file.path + File.separator)) {
                "Symlink escapes rootfs: ${target.name}"
            }
            (target.parentFile ?: root).toPath().relativize(resolved.toPath()).toFile()
        }
        target.delete()
        Files.createSymbolicLink(target.toPath(), link_target.toPath())
    }

    private fun create_hard_link(root: File, target: File, link_name: String) {
        if (link_name.isBlank()) return
        val source = root.safe_resolve(link_name)
        if (!source.exists()) return
        target.delete()
        runCatching {
            Files.createLink(target.toPath(), source.toPath())
        }.recoverCatching { error ->
            if (error !is IOException && error !is UnsupportedOperationException && error !is SecurityException) throw error
            source.copyTo(target, overwrite = true)
            target.setReadable(source.canRead(), false)
            target.setWritable(source.canWrite(), true)
            target.setExecutable(source.canExecute(), false)
        }.getOrThrow()
    }

    private fun InputStream.read_tar_header(): tar_header? {
        val header = ByteArray(tar_block_size)
        val read = read_fully_or_end(header)
        if (read == 0) return null
        if (read < tar_block_size) throw EOFException("Unexpected EOF while reading tar header")
        if (header.all { it == 0.toByte() }) return null

        val name = header.string(0, 100)
        val prefix = header.string(345, 155)
        val full_name = listOf(prefix, name).filter { it.isNotBlank() }.joinToString("/")
        return tar_header(
            name = normalize_tar_path(full_name),
            mode = header.octal(100, 8).toInt(),
            size = header.octal(124, 12),
            mod_time = header.octal(136, 12),
            type = when (header[156].toInt().toChar()) {
                '0', '\u0000' -> tar_entry_type.FILE
                '5' -> tar_entry_type.DIRECTORY
                '2' -> tar_entry_type.SYMLINK
                '1' -> tar_entry_type.HARDLINK
                'L' -> tar_entry_type.LONG_NAME
                'K' -> tar_entry_type.LONG_LINK
                'x' -> tar_entry_type.PAX
                else -> tar_entry_type.OTHER
            },
            link_name = header.string(157, 100)
        )
    }

    private fun parse_pax(text: String): Map<String, String> {
        val result = mutableMapOf<String, String>()
        var index = 0
        while (index < text.length) {
            val space = text.indexOf(' ', index)
            if (space < 0) break
            val length = text.substring(index, space).toIntOrNull() ?: break
            val end = (index + length).coerceAtMost(text.length)
            val record = text.substring(space + 1, end).trimEnd('\n')
            val equals = record.indexOf('=')
            if (equals > 0) result[record.substring(0, equals)] = record.substring(equals + 1)
            index += length
        }
        return result
    }

    private fun check_interrupted() {
        if (Thread.currentThread().isInterrupted) throw InterruptedException("Rootfs install cancelled")
    }

    private fun InputStream.copy_exactly(output: java.io.OutputStream, bytes: Long) {
        val buffer = ByteArray(buffer_size)
        var remaining = bytes
        while (remaining > 0) {
            check_interrupted()
            val read = read(buffer, 0, minOf(buffer.size.toLong(), remaining).toInt())
            if (read < 0) throw EOFException("Unexpected EOF while extracting tar entry")
            output.write(buffer, 0, read)
            remaining -= read
        }
    }

    private fun InputStream.read_exactly(bytes: Long): ByteArray {
        require(bytes <= Int.MAX_VALUE) { "Tar entry is too large to buffer: $bytes" }
        val buffer = ByteArray(bytes.toInt())
        val read = read_fully_or_end(buffer)
        if (read != buffer.size) throw EOFException("Unexpected EOF while reading tar entry")
        return buffer
    }

    private fun InputStream.skip_fully(bytes: Long) {
        var remaining = bytes
        while (remaining > 0) {
            check_interrupted()
            val skipped = skip(remaining)
            if (skipped > 0) remaining -= skipped else if (read() >= 0) remaining-- else throw EOFException("Unexpected EOF while skipping tar data")
        }
    }

    private fun InputStream.read_fully_or_end(buffer: ByteArray): Int {
        var offset = 0
        while (offset < buffer.size) {
            val read = read(buffer, offset, buffer.size - offset)
            if (read < 0) break
            offset += read
        }
        return offset
    }

    private fun File.safe_resolve(path: String): File {
        val normalized = normalize_tar_path(path)
        val root = canonicalFile
        val target = File(root, normalized).canonicalFile
        require(target.path == root.path || target.path.startsWith(root.path + File.separator)) {
            "Rootfs entry escapes target directory: $path"
        }
        return target
    }

    private fun File.apply_mode(mode: Int) {
        setReadable(mode and 0b100_000_000 != 0, false)
        setWritable(mode and 0b010_000_000 != 0, true)
        setExecutable(mode and 0b001_000_000 != 0, false)
    }

    private fun normalize_tar_path(path: String): String {
        val normalized = path.replace('\\', '/').trim().trimStart('/').removePrefix("./")
        require(normalized.isNotBlank()) { "Rootfs entry path is blank" }
        require(!normalized.contains('\u0000')) { "Rootfs entry path contains invalid character" }
        require(normalized.split('/').none { it == ".." }) { "Rootfs entry escapes target directory: $path" }
        return normalized
    }

    private fun ByteArray.string(offset: Int, length: Int): String {
        val end = (offset until offset + length).firstOrNull { this[it] == 0.toByte() } ?: (offset + length)
        return copyOfRange(offset, end).toString(Charsets.UTF_8).trim()
    }

    private fun ByteArray.octal(offset: Int, length: Int): Long {
        val value = string(offset, length).trim().lowercase(Locale.US).trimEnd('\u0000')
        return if (value.isBlank()) 0L else value.toLong(8)
    }

    private fun Long.padding_size(): Long = (tar_block_size - (this % tar_block_size)).let { if (it == tar_block_size.toLong()) 0L else it }

    private fun Long.padded_tar_size(): Long = this + padding_size()

    private data class tar_header(
        val name: String,
        val mode: Int,
        val size: Long,
        val mod_time: Long,
        val type: tar_entry_type,
        val link_name: String
    )

    private enum class tar_entry_type {
        FILE,
        DIRECTORY,
        SYMLINK,
        HARDLINK,
        LONG_NAME,
        LONG_LINK,
        PAX,
        OTHER
    }

    private enum class archive_format {
        TAR_GZ {
            override fun wrap_stream(input: InputStream): InputStream = GZIPInputStream(input)
        },
        TAR_XZ {
            override fun wrap_stream(input: InputStream): InputStream = XZInputStream(input)
        };

        abstract fun wrap_stream(input: InputStream): InputStream

        companion object {
            fun from_file(file: File): archive_format {
                val name = file.name.substringBefore('?').substringBefore('#').lowercase(Locale.US)
                return when {
                    name.endsWith(".tar.xz") || name.endsWith(".txz") -> TAR_XZ
                    name.endsWith(".tar.gz") || name.endsWith(".tgz") -> TAR_GZ
                    else -> TAR_GZ
                }
            }
        }
    }

    private companion object {
        private const val tar_block_size = 512
        private const val buffer_size = 64 * 1024
        private const val connect_timeout_ms = 30_000
        private const val read_timeout_ms = 60_000
    }
}

fun format_rootfs_size(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "%.2f KB".format(bytes / 1024.0)
        bytes < 1024 * 1024 * 1024 -> "%.2f MB".format(bytes / (1024.0 * 1024))
        else -> "%.2f GB".format(bytes / (1024.0 * 1024 * 1024))
    }
}

fun format_rootfs_speed(bytes_per_sec: Long): String {
    return when {
        bytes_per_sec < 1024 -> "$bytes_per_sec B/s"
        bytes_per_sec < 1024 * 1024 -> "%.2f KB/s".format(bytes_per_sec / 1024.0)
        else -> "%.2f MB/s".format(bytes_per_sec / (1024.0 * 1024))
    }
}
