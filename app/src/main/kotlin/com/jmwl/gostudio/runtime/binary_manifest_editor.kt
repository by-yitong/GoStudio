package com.jmwl.gostudio.runtime

import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * 极简 AndroidManifest.xml 二进制（AXML）字符串池改写器。
 *
 * 原理：AXML 文件由 chunk 序列组成，文件头（RES_XML_TYPE=0x0003，8 字节）之后
 * 第一个 chunk 是全局字符串池 (RES_STRING_POOL_TYPE=0x0001)。package / label /
 * versionName 等属性的值都以字符串形式存在这个池里。替换池中的字符串并重建
 * 文件头与池 chunk 即可完成改写；后续 chunk 使用相对偏移，整体平移安全。
 */
object binary_manifest_editor {

    private const val CHUNK_HEADER_SIZE = 8
    private const val RES_STRING_POOL_TYPE = 0x0001
    private const val RES_XML_START_ELEMENT_TYPE = 0x0102
    private const val UTF8_FLAG = 1 shl 8
    private const val SORTED_FLAG = 1 shl 1

    /**
     * 替换 manifest 中所有命中的字符串（等值替换，如 "App" -> "我的应用"）。
     * manifest_bytes: 原 AndroidManifest.xml 二进制内容
     * replacements: 旧串 -> 新串（精确匹配整个字符串条目）
     * prefix_replacements: 前缀替换（如 "com.jmwl.gostudio.shell" -> "com.gs.app"），
     *   命中 "com.jmwl.gostudio.shell.androidx-startup" 这类带壳包名前缀的
     *   provider 授权/权限声明串；不替换它们的话，不同项目的 APK 会因
     *   authorities 冲突无法共存（INSTALL_FAILED_CONFLICTING_PROVIDER）。
     * protected_strings: 明确不做前缀替换的串（如壳 Activity 的真实类名，
     *   它必须与 DEX 里的类一致，不能跟着包名换）。
     */
    fun replace_strings(
        manifest_bytes: ByteArray,
        replacements: Map<String, String>,
        prefix_replacements: Map<String, String> = emptyMap(),
        protected_strings: Set<String> = emptySet()
    ): ByteArray {
        if (replacements.isEmpty() && prefix_replacements.isEmpty()) return manifest_bytes
        val buffer = ByteBuffer.wrap(manifest_bytes).order(ByteOrder.LITTLE_ENDIAN)

        // 文件级 header: type(2) headerSize(2) size(4)
        val file_type = buffer.short.toInt() and 0xFFFF
        val file_header_size = buffer.short.toInt() and 0xFFFF
        val file_size = buffer.int
        check(file_type == 0x0003) { "不是二进制 AndroidManifest.xml" }
        check(file_size == manifest_bytes.size) { "AndroidManifest.xml 文件长度字段与实际不符" }

        // 文件头之后第一个 chunk 必须是字符串池
        val pool_type = buffer.short.toInt() and 0xFFFF
        val pool_header_size = buffer.short.toInt() and 0xFFFF
        val pool_size = buffer.int
        check(pool_type == RES_STRING_POOL_TYPE) { "AXML 第一个 chunk 不是字符串池" }
        check(pool_header_size == 28) { "字符串池 headerSize 不是 28" }

        val string_count = buffer.int
        val style_count = buffer.int
        val flags = buffer.int
        val strings_start = buffer.int
        val styles_start = buffer.int

        val is_utf8 = (flags and UTF8_FLAG) != 0

        // 读取字符串偏移表
        val offsets = IntArray(string_count)
        for (i in 0 until string_count) offsets[i] = buffer.int

        // 字符串数据区：stringsStart/stylesStart 均相对池 chunk 开头（chunk 起于文件偏移 8）
        val strings_abs = CHUNK_HEADER_SIZE + strings_start
        val styles_abs = if (styles_start != 0) CHUNK_HEADER_SIZE + styles_start else CHUNK_HEADER_SIZE + pool_size
        val data = manifest_bytes.copyOfRange(strings_abs, styles_abs)

        // 按偏移表定位每条字符串（含长度前缀）。
        // 不能从数据区头盲走：字符串区末尾可能有对齐填充字节，会被误当成下一条。
        val strings = mutableListOf<String>()
        val encoded = mutableListOf<ByteArray>()
        for (i in 0 until string_count) {
            val start = offsets[i]
            check(start in 0 until data.size) { "字符串偏移越界: $start" }
            val (text, len, _) = read_string(data, start, is_utf8)
            check(start + len <= data.size) { "字符串条目越界: ${start + len} > ${data.size}" }
            strings.add(text)
            encoded.add(data.copyOfRange(start, start + len))
        }

        // 替换：先精确匹配，再按前缀换壳包名（受保护串除外）
        var changed = false
        for (i in strings.indices) {
            val exact = replacements[strings[i]]
            val next = exact ?: prefix_swap(strings[i], prefix_replacements, protected_strings)
            if (next != null && next != strings[i]) {
                encoded[i] = encode_string(next, is_utf8)
                strings[i] = next
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
        // 4 字节对齐（池内所有区块都必须 4 字节对齐）
        while (new_data.size() % 4 != 0) new_data.write(0)

        // styles 区原样保留（manifest 通常 styleCount=0，但按规范处理）
        val styles_copy = if (styles_start != 0) {
            val padded = ByteArrayOutputStream()
            padded.write(manifest_bytes.copyOfRange(styles_abs, CHUNK_HEADER_SIZE + pool_size))
            while (padded.size() % 4 != 0) padded.write(0)
            padded.toByteArray()
        } else ByteArray(0)

        val new_strings_start = 28 + string_count * 4 // header(28) + offsets
        val new_styles_start = new_strings_start + new_data.size()
        val new_pool_size = new_styles_start + styles_copy.size
        val new_flags = (flags and SORTED_FLAG.inv()) // 保留 UTF8 位

        // 后续 chunk（XML 节点等）偏移都是相对自身的，随池长度变化整体平移安全
        val rest = manifest_bytes.copyOfRange(CHUNK_HEADER_SIZE + pool_size, manifest_bytes.size)
        val new_file_size = CHUNK_HEADER_SIZE + new_pool_size + rest.size

        val out = ByteArrayOutputStream()
        fun write_short(v: Int) { out.write(v and 0xFF); out.write((v shr 8) and 0xFF) }
        fun write_int(v: Int) {
            out.write(v and 0xFF); out.write((v shr 8) and 0xFF); out.write((v shr 16) and 0xFF); out.write((v shr 24) and 0xFF)
        }
        // 文件级头（RES_XML_TYPE + 全文件长度）——池 chunk 从偏移 8 开始，不能顶替文件头
        write_short(file_type)
        write_short(file_header_size)
        write_int(new_file_size)
        // 字符串池 chunk
        write_short(RES_STRING_POOL_TYPE); write_short(28)
        write_int(new_pool_size)
        write_int(string_count)
        write_int(style_count)
        write_int(new_flags)
        write_int(new_strings_start)
        write_int(if (styles_start != 0) new_styles_start else 0)
        for (offset in new_offsets) write_int(offset)
        new_data.writeTo(out)
        out.write(styles_copy)
        out.write(rest)
        return out.toByteArray()
    }

    /** 前缀替换：串以某个 key 开头时换掉前缀；受保护串不动。 */
    private fun prefix_swap(
        text: String,
        prefix_replacements: Map<String, String>,
        protected_strings: Set<String>
    ): String? {
        if (prefix_replacements.isEmpty() || text in protected_strings) return null
        for ((old, new) in prefix_replacements) {
            if (text.startsWith(old)) return new + text.removePrefix(old)
        }
        return null
    }

    /**
     * 改写 manifest 根元素上的 android:versionCode（整型属性，不改文件长度）。
     * 属性值是 typedValue.data（INT_DEC），直接原地覆写；
     * rawValue 若有原始字符串形式也一并替换，保证 aapt 之类工具读到的值一致。
     */
    fun set_version_code(manifest_bytes: ByteArray, version_code: Int): ByteArray {
        check(version_code in 1..2_000_000_000) { "versionCode 超出范围: $version_code" }
        val out = manifest_bytes.copyOf()
        val buffer = ByteBuffer.wrap(out).order(ByteOrder.LITTLE_ENDIAN)

        // 解析字符串池拿字符串表（属性名按索引引用池内字符串）
        val pool = parse_string_pool(buffer) ?: return manifest_bytes

        // 遍历 chunk，找 <manifest> 元素的 START chunk
        var pos = CHUNK_HEADER_SIZE
        while (pos + CHUNK_HEADER_SIZE <= out.size) {
            val type = read_u16(out, pos)
            val header_size = read_u16(out, pos + 2)
            val size = read_u32(out, pos + 4)
            if (size <= 0 || pos + size > out.size) break
            if (type == RES_XML_START_ELEMENT_TYPE) {
                // XML 节点 chunk 头是 16 字节（chunk 头 8 + lineNumber 4 + comment 4），
                // attrExt 从 headerSize 处开始
                val ext = pos + header_size
                val element_name = pool.getOrNull(read_u32(out, ext + 4).toInt()) ?: ""
                if (element_name == "manifest") {
                    patch_version_code_attribute(out, ext, pool, version_code)
                    return out
                }
            }
            pos += size
        }
        return manifest_bytes
    }

    private fun patch_version_code_attribute(out: ByteArray, ext: Int, pool: List<String>, version_code: Int) {
        val attribute_start = read_u16(out, ext + 8)
        val attribute_size = read_u16(out, ext + 10)
        val attribute_count = read_u16(out, ext + 12)
        val attr_base = ext + attribute_start
        for (i in 0 until attribute_count) {
            val attr = attr_base + i * attribute_size
            val name = pool.getOrNull(read_u32(out, attr + 4).toInt()) ?: continue
            if (name != "versionCode") continue
            // ResXMLTree_attribute: ns(4) name(4) rawValue(4) size(2) res0(1) dataType(1) data(4)
            // 安装器读 typedValue.data（INT_DEC）；rawValue 通常无原始字面量，不做处理
            if ((out[attr + 15].toInt() and 0xFF) == 0x10) {
                write_u32(out, attr + 16, version_code)
            }
        }
    }

    /** 解析字符串池：返回池内字符串列表（只读，不重建）。 */
    private fun parse_string_pool(buffer: ByteBuffer): List<String>? {
        buffer.position(0)
        // 文件级头: type(2) headerSize(2) size(4)，之后紧跟字符串池 chunk
        val file_type = buffer.short.toInt() and 0xFFFF
        check(file_type == 0x0003) { "不是二进制 AndroidManifest.xml" }
        buffer.short // 文件头 headerSize
        buffer.int   // 文件头 size

        val pool_type = buffer.short.toInt() and 0xFFFF
        val pool_header_size = buffer.short.toInt() and 0xFFFF
        val pool_size = buffer.int
        check(pool_type == RES_STRING_POOL_TYPE) { "AXML 第一个 chunk 不是字符串池" }
        check(pool_header_size == 28) { "字符串池 headerSize 不是 28" }

        val string_count = buffer.int
        val style_count = buffer.int
        val flags = buffer.int
        val strings_start = buffer.int
        buffer.int // stylesStart，这里用不到

        val is_utf8 = (flags and UTF8_FLAG) != 0
        val offsets = IntArray(string_count)
        for (i in 0 until string_count) offsets[i] = buffer.int

        val strings_abs = CHUNK_HEADER_SIZE + strings_start
        val data = ByteArray(pool_size - strings_start)
        System.arraycopy(buffer.array(), strings_abs, data, 0, data.size)

        val strings = mutableListOf<String>()
        for (i in 0 until string_count) {
            val (text, _, _) = read_string(data, offsets[i], is_utf8)
            strings.add(text)
        }
        return strings
    }

    private fun read_u16(data: ByteArray, pos: Int): Int =
        (data[pos].toInt() and 0xFF) or ((data[pos + 1].toInt() and 0xFF) shl 8)

    private fun read_u32(data: ByteArray, pos: Int): Int =
        (data[pos].toInt() and 0xFF) or ((data[pos + 1].toInt() and 0xFF) shl 8) or
            ((data[pos + 2].toInt() and 0xFF) shl 16) or ((data[pos + 3].toInt() and 0xFF) shl 24)

    private fun write_u32(data: ByteArray, pos: Int, value: Int) {
        data[pos] = (value and 0xFF).toByte()
        data[pos + 1] = ((value shr 8) and 0xFF).toByte()
        data[pos + 2] = ((value shr 16) and 0xFF).toByte()
        data[pos + 3] = ((value shr 24) and 0xFF).toByte()
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
            // 高位 uint16 = 0x8000 | (v >> 16)，低位 uint16 = v & 0xFFFF，均小端
            out.write(((v shr 16) and 0x7F) or 0x80)
            out.write(((v shr 16) shr 8) or 0x80)
            out.write(v and 0xFF); out.write((v shr 8) and 0xFF)
        } else {
            out.write(v and 0xFF); out.write((v shr 8) and 0xFF)
        }
    }
}
