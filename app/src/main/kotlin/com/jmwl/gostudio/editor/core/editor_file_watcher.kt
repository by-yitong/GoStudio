package com.jmwl.gostudio.editor.core

import android.os.FileObserver

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * 项目文件监听器：基于 inotify（[FileObserver]）的按目录监听。
 *
 * FileObserver 只能监听单个目录（不含子目录），而文件树是懒加载的，
 * 因此这里不做整树递归挂载，只监听「文件树已缓存（根目录+已展开）+
 * 打开 tab 的父目录」，由外部随展开/开关 tab 调用 [set_directories] 全量同步。
 *
 * 终端（proot）里的 git pull / touch / rm 写的是同一份宿主文件，
 * inotify 事件照常到达，外部新建/删除/修改都能实时感知。
 *
 * 事件在 FileObserver 线程到达，先累积、静默 [debounce_ms] 后在
 * scope（主线程）一次性回调，避免 git pull 之类的事件风暴打爆刷新。
 */
class editor_file_watcher(
    private val scope: CoroutineScope,
    private val debounce_ms: Long = 400L,
    private val on_changed: (changed_files: Set<String>, tree_dirs: Set<String>) -> Unit
) {
    private val lock = Any()
    private val observers = HashMap<String, FileObserver>()
    private val pending_files = ConcurrentHashMap.newKeySet<String>()
    private val pending_dirs = ConcurrentHashMap.newKeySet<String>()
    private val tick = Channel<Unit>(Channel.CONFLATED)
    private var collect_job: Job? = null

    fun start() {
        if (collect_job?.isActive == true) return
        collect_job = scope.launch {
            while (isActive) {
                tick.receive()
                // 事件风暴期间每个新事件都顺延静默期，静默期满才落地一次
                while (true) {
                    if (withTimeoutOrNull(debounce_ms) { tick.receive() } == null) break
                }
                flush()
            }
        }
    }

    fun stop() {
        collect_job?.cancel()
        collect_job = null
        synchronized(lock) {
            observers.values.forEach { it.stopWatching() }
            observers.clear()
        }
        pending_files.clear()
        pending_dirs.clear()
    }

    /** 全量同步监听目录集合：新增的挂载，多余的卸载。 */
    fun set_directories(dirs: Set<String>) {
        synchronized(lock) {
            val wanted = dirs.filterTo(HashSet()) { File(it).isDirectory }
            (observers.keys - wanted).forEach { remove_observer_locked(it) }
            (wanted - observers.keys).forEach { add_observer_locked(it) }
        }
    }

    private fun add_observer_locked(path: String) {
        val observer = object : FileObserver(path, WATCH_MASK) {
            override fun onEvent(event: Int, rel_path: String?) {
                handle_event(path, event, rel_path)
            }
        }
        observers[path] = observer
        observer.startWatching()
    }

    private fun remove_observer_locked(path: String) {
        observers.remove(path)?.stopWatching()
    }

    private fun handle_event(dir_path: String, event: Int, rel_path: String?) {
        if (event and (FileObserver.DELETE_SELF or FileObserver.MOVE_SELF) != 0) {
            synchronized(lock) { remove_observer_locked(dir_path) }
            pending_dirs.add(dir_path)
            tick.trySend(Unit)
            return
        }
        if (rel_path.isNullOrBlank()) return
        val full_path = File(dir_path, rel_path).absolutePath
        if (event and TREE_EVENT_MASK != 0) pending_dirs.add(dir_path)
        if (event and FILE_EVENT_MASK != 0) pending_files.add(full_path)
        if (pending_dirs.isNotEmpty() || pending_files.isNotEmpty()) tick.trySend(Unit)
    }

    private fun flush() {
        val files = pending_files.toHashSet()
        pending_files.removeAll(files)
        val dirs = pending_dirs.toHashSet()
        pending_dirs.removeAll(dirs)
        if (files.isEmpty() && dirs.isEmpty()) return
        runCatching { on_changed(files, dirs) }
    }

    private companion object {
        // 目录结构变化（新建/删除/移动条目）→ 文件树需要刷新
        val TREE_EVENT_MASK = FileObserver.CREATE or FileObserver.DELETE or
                FileObserver.MOVED_TO or FileObserver.MOVED_FROM
        // 内容可能被改写（CLOSE_WRITE=写入关闭，MODIFY=写入中，MOVED_TO/CREATE=新文件落地）
        val FILE_EVENT_MASK = FileObserver.CLOSE_WRITE or FileObserver.MODIFY or
                FileObserver.ATTRIB or FileObserver.MOVED_TO or FileObserver.CREATE
        val WATCH_MASK = TREE_EVENT_MASK or FILE_EVENT_MASK or
                FileObserver.DELETE_SELF or FileObserver.MOVE_SELF
    }
}
