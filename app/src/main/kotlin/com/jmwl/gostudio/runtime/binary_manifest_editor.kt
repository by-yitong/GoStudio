package com.jmwl.gostudio.runtime

import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * 极简 AndroidManifest.xml 二进制（AXML）字符串池改写器。
 *
 * 原理：AXML 文件由 chunk 序列组成，第一个 chunk 是全局字符串池
 * (RES_STRING_POOL_TYPE=0x0001)。package / label / versionName 等属性
 * 的值都以字符串形式存在这个池里。替换池中的字符串并重建该 chunk
 * 即可完成改写；后续 chunk 使用相对偏移，整体平移安全。
 */
object binary_manifest_editor {

    private const val CHUNK_HEADER_SIZE = 8
    private const val RES_STRING_POOL_TYPE = 0x0001
    private const val UTF8_FLAG = 1 shl 8
    private const val SORTED_FLAG = 1 shl 1

    /**
     * 替换 manifest 中所有命中的字符串（等值替换，如 "App" -> "我的应用"）。
     * manifest_bytes: 原 AndroidManifest.xml 二进制内容
     * replacements: 旧串 -> 新串（精确匹配整个字符串条目）
     */
    fun replace_strings(manifest_bytes: ByteArray, replacements: Map<String, String>): ByteArray {
        if (replacements.isEmpty()) return manifest_bytes
        val buffer = ByteBuffer.wrap(manifest_bytes).order(ByteOrder.LITTLE_ENDIAN)

        // 文件级 header: type(2) headerSize(2) size(4)
        val file_type = buffer.short.toInt() and 0xFFFF
        val file_header_size = buffer.short.toInt() and 0xFFFF
        val file_size = buffer.int
        check(file_type == 0x0003) { "不是二进制 AndroidManifest.xml" }

        // 第一个 chunk 必须是字符串池
        val pool_type = buffer.short.toInt() and 0xFFFF
        val pool_header_size = buffer.short.toInt() and 0xFFFF
        val pool_size = buffer.int
        check(pool_type == RES_STRING_POOL_TYPE) { "AXML 第一个 chunk 不是字符串池" }

        val string_count = buffer.int
        val style_count = buffer.int
        val flags = buffer.int
        val strings_start = buffer.int
        val styles_start = buffer.int

        val is_utf8 = (flags and UTF8_FLAG) != 0
        val base_offset = buffer.position() // 已读完 pool 通用头（28字节处）

        // 读取字符串偏移表
        val offsets = IntArray(string_count)
        for (i in 0 until string_count) offsets[i] = buffer.int

        // 字符串数据区
        val data_start = base_offset + string_count * 4
        val string_data = manifest_bytes.copyOfRange(
            CHUNK_HEADER_SIZE + strings_start,
            if (styles_start != 0) CHUNK_HEADER_SIZE + styles_start else CHUNK_HEADER_SIZE + pool_size - CHUNK_HEADER_SIZE
        )
        // 上面的区间基于绝对位置：stringsStart 相对 chunk 开头
        // 重新按规范计算
        val strings_abs = CHUNK_HEADER_SIZE + strings_start
        val styles_abs = if (styles_start != 0) CHUNK_HEADER_SIZE + styles_start else CHUNK_HEADER_SIZE + pool_size
        val data = manifest_bytes.copyOfRange(strings_abs, styles_abs)

        // 解析全部字符串（含长度前缀）
        val strings = mutableListOf<String>()
        val encoded = mutableListOf<ByteArray>()
        var pos = 0
        while (pos < data.size) {
            val (text, len, _) = read_string(data, pos, is_utf8)
            strings.add(text)
            encoded.add(data.copyOfRange(pos, pos + len))
            pos += len
        }

        // 替换
        var changed = false
        for (i in strings.indices) {
            val replacement = replacements[strings[i]]
            if (replacement != null && replacement != strings[i]) {
                encoded[i] = encode_string(replacement, is_utf8)
                strings[i] = replacement
                changed = true
            }
        }
        if (!changed) return manifest_bytes

        // 重建字符串数据区（保持原顺序；不排序，清 SORTED 位保证安全）
        val new_data = ByteArrayOutputStream()
        val new_offsets = IntArray(encoded.size)
        for (i in encoded.indices) {
            new_offsets[i] = new_data.size()
            new_data.write(encoded[i])
        }
        // 4 字节对齐
        while (new_data.size() % 4 != 0) new_data.write(0)

        val new_strings_start = 28 + string_count * 4 // header(28) + offsets
        val new_pool_size = new_strings_start + new_data.size()
        val new_flags = (flags and SORTED_FLAG.inv()) // 保留 UTF8 位

        // 重建字符串池 chunk
        val rest = manifest_bytes.copyOfRange(CHUNK_HEADER_SIZE + pool_size, manifest_bytes.size)
        val out = ByteArrayOutputStream()
        fun write_short(v: Int) { out.write(v and 0xFF); out.write((v shr 8) and 0xFF) }
        fun write_int(v: Int) {
            out.write(v and 0xFF); out.write((v shr 8) and 0xFF)
            out.write((v shr 16) and 0xFF); out.write((v shr 24) and 0xFF)
        }
        write_short(RES_STRING_POOL_TYPE); write_short(28)
        write_int(new_pool_size)
        write_int(string_count)
        write_int(style_count)
        write_int(new_flags)
        write_int(new_strings_start)
        write_int(if (styles_start != 0) new_pool_size else 0)
        for (offset in new_offsets) write_int(offset)
        new_data.writeTo(out)
        out.write(rest)

        val result = out.toByteArray()
        // 修正文件级 size
        val fix = ByteBuffer.wrap(result).order(ByteOrder.LITTLE_ENDIAN)
        fix.position(4)
        fix.putInt(result.size)
        return result
    }

    /** 读一个字符串条目：返回 (文本, 总字节数含长度前缀, ...) */
    private fun read_string(data: ByteArray, pos: Int, utf8: Boolean): Triple<String, Int, Unit> {
        var p = pos
        val text: String
        if (utf8) {
            // UTF-8: u16len(1-2字节), u8len(1-2字节), bytes, 0x00
            val (u16len, s1) = read_len8(data, p); p += s1
            val (u8len, s2) = read_len8(data, p); p += s2
            text = String(data, p, u8len, Charsets.UTF_8)
            p += u8len + 1 // 结尾 0x00
        } else {
            val (len, s1) = read_len16(data, p); p += s1
            val byte_len = len * 2
            text = String(data, p, byte_len, Charsets.UTF_16LE)
            p += byte_len + 2
        }
        return Triple(text, p - pos, Unit)
    }

    private fun read_len8(data: ByteArray, pos: Int): Pair<Int, Int> {
        val first = data[pos].toInt() and 0xFF
        return if (first and 0x80 != 0) {
            ((first and 0x7F) shl 8) or (data[pos + 1].toInt() and 0xFF) to 2
        } else first to 1
    }

    private fun read_len16(data: ByteArray, pos: Int): Pair<Int, Int> {
        val first = ((data[pos].toInt() and 0xFF) or ((data[pos + 1].toInt() and 0xFF) shl 8))
        return if (first and 0x8000 != 0) {
            ((first and 0x7FFF) shl 16) or (
                (data[pos + 2].toInt() and 0xFF) or ((data[pos + 3].toInt() and 0xFF) shl 8)
            ) to 4
        } else first to 2
    }

    private fun encode_string(text: String, utf8: Boolean): ByteArray {
        val out = ByteArrayOutputStream()
        if (utf8) {
            val bytes = text.toByteArray(Charsets.UTF_8)
            write_len8(out, text.length)
            write_len8(out, bytes.size)
            out.write(bytes)
            out.write(0)
        } else {
            write_len16(out, text.length)
            out.write(text.toByteArray(Charsets.UTF_16LE))
            out.write(0); out.write(0)
        }
        return out.toByteArray()
    }

    private fun write_len8(out: ByteArrayOutputStream, v: Int) {
        if (v > 0x7F) { out.write(((v shr 8) or 0x80)); out.write(v and 0xFF) }
        else out.write(v)
    }

    private fun write_len16(out: ByteArrayOutputStream, v: Int) {
        if (v > 0x7FFF) {
            out.write(((v shr 16) or 0x80) and 0xFF); out.write((v shr 16) shr 8)
            out.write(v and 0xFF); out.write((v shr 8) and 0xFF)
        } else {
            out.write(v and 0xFF); out.write((v shr 8) and 0xFF)
        }
    }
}
