package com.jmwl.gostudio.toolchain

/** 单个提交的图输入：hash 与父提交 hash 列表（新→旧顺序传入）。 */
data class git_commit_node(val hash: String, val parents: List<String>)

/**
 * 一行的图输出。
 *
 * @param node_lane 本提交节点所在泳道号（从 0 起）
 * @param active_lanes 该行内有竖线墨迹的泳道号（升序去重）＝进入该行的泳道 ∪ 本行向下延续的泳道，
 *   供绘制「顶边→节点」「穿过整行」「节点→底边」三类半段/整段竖线
 * @param is_merge 父数 ≥2（合并提交，渲染时画双圈）
 */
data class git_graph_row(
    val node_lane: Int,
    val active_lanes: List<Int>,
    val is_merge: Boolean
)

/**
 * 提交历史泳道拓扑的纯算法（无 Android 依赖，可 JUnit 直测）。
 *
 * 输入为 log 新→旧顺序的提交序列，输出逐行泳道布局，供 GitKraken 风格的
 * Canvas 泳道图（每行画自己的节点与上下半段连线）使用。
 *
 * 算法（单遍扫描，O(n·泳道数)）：
 * - 维护「预占表」hash→lane：上层某提交为它在下方还未出现的父提交预留的泳道；
 *   预占表的取值集合即「进入当前行的活跃泳道」。
 * - 提交节点落在其被预占的泳道上；未被预占（分支顶端）则分配最小未占泳道。
 * - 首父继承本提交的泳道（若首父已被别的线预占则并入那条线，本线收敛）；
 *   其余父各自分配最小未占泳道（分叉）。
 * - 父不在已加载窗口内（浅克隆边界）或无父（根提交）时不预占，该线就此终结；
 *   终结后泳道号立即回收，可被后续最小未占分配复用。
 *
 * active_lanes 语义（渲染契约）：
 * - 第 i 行 active_lanes =（进入第 i 行时已预占的泳道）∪（第 i 行处理完后仍预占的泳道）。
 *   前者只出现在集合里 → 只画「顶边→节点」上半段；两者都在 → 画整段穿过后继续；
 *   只在后者 → 是本行新生分叉/新顶，画「节点→底边」下半段。
 * - 相邻行连续性：泳道只会在某行的节点处终结（且必是该行 node_lane），
 *   也只会由某行自己的节点/父分叉新生，因此
 *   `active[i] - active[i+1] ⊆ {node_lane[i]}`，
 *   `|active[i+1] - active[i]| ≤ 第 i+1 个提交的窗口内父数`，
 *   两条不变式保证竖线跨行衔接不悬空（见单测 continuity 用例）。
 */
object git_graph_builder {

    /**
     * commits 按时间新→旧排列；parents 指向更早的提交。
     * 返回与 commits 等长等序的逐行布局；空输入返回空列表。
     */
    fun build(commits: List<git_commit_node>): List<git_graph_row> {
        if (commits.isEmpty()) return emptyList()

        // 已加载窗口内的全部 hash；父不在此集合 → 浅克隆边界，视为 lane 终结
        val window = HashSet<String>(commits.size * 2)
        for (commit in commits) window.add(commit.hash)

        // 预占表：父 hash → 它将出现的泳道；其取值集合即当前活跃泳道
        val reserved_lane = HashMap<String, Int>()
        val occupied = HashSet<Int>()

        val rows = ArrayList<git_graph_row>(commits.size)

        // 最小未占泳道号
        fun min_free_lane(): Int {
            var lane = 0
            while (lane in occupied) lane++
            return lane
        }

        for (commit in commits) {
            // 进入本行的活跃泳道（上一行向下延续至此的竖线，含穿过本行的）
            val entering = HashSet(occupied)

            // 本提交节点：被预占则落在该泳道，否则（分支顶端）分配最小未占泳道
            val claimed = reserved_lane.remove(commit.hash)
            val node_lane = if (claimed != null) {
                occupied.remove(claimed)
                claimed
            } else {
                min_free_lane()
            }

            // 为父提交预占泳道：首父继承 node_lane，其余父各开新泳道；
            // 父已在线上（被预占）则并入既有线（合并收敛）；窗口外父不预占
            for ((index, parent) in commit.parents.withIndex()) {
                if (parent !in window) continue
                if (reserved_lane.containsKey(parent)) continue
                val lane = if (index == 0) node_lane else min_free_lane()
                reserved_lane[parent] = lane
                occupied.add(lane)
            }

            entering.addAll(occupied)
            rows.add(
                git_graph_row(
                    node_lane = node_lane,
                    active_lanes = entering.sorted(),
                    is_merge = commit.parents.size >= 2
                )
            )
        }
        return rows
    }
}
