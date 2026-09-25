package com.jmwl.gostudio.tools

import com.google.common.truth.Truth.assertThat
import com.jmwl.gostudio.toolchain.git_commit_node
import com.jmwl.gostudio.toolchain.git_graph_builder
import org.junit.Test

/**
 * 提交历史泳道拓扑算法回归：
 * 泳道号直接决定 Canvas 落笔位置，任何偏移都会画出错线/悬空线，
 * 这里锁住线性、分叉、合并、回收复用、窗口边界、连续性与性能。
 */
class git_graph_builder_test {

    private fun node(hash: String, vararg parents: String) =
        git_commit_node(hash, parents.toList())

    private fun build(vararg commits: git_commit_node) =
        git_graph_builder.build(commits.toList())

    @Test
    fun `linear chain keeps single lane`() {
        // c3 → c2 → c1(根)
        val rows = build(node("c3", "c2"), node("c2", "c1"), node("c1"))

        assertThat(rows).hasSize(3)
        // 全部节点在 0 号泳道
        assertThat(rows.map { it.node_lane }).containsExactly(0, 0, 0).inOrder()
        assertThat(rows.map { it.is_merge }).containsExactly(false, false, false).inOrder()

        // 每行 active_lanes 逐行正确：前两行线穿过整行，最后一行只剩「顶边→节点」的自身半段
        assertThat(rows[0].active_lanes).containsExactly(0).inOrder()
        assertThat(rows[1].active_lanes).containsExactly(0).inOrder()
        assertThat(rows[2].active_lanes).containsExactly(0).inOrder()
    }

    @Test
    fun `fork puts children on different lanes`() {
        // A → (B, C)，log 顺序 B、C、A
        val rows = build(node("B", "A"), node("C", "A"), node("A"))

        assertThat(rows).hasSize(3)
        // B 在 0 号道，C 是新顶端开 1 号道，A 回到其被预占的 0 号道
        assertThat(rows.map { it.node_lane }).containsExactly(0, 1, 0).inOrder()

        // C 行：0 号道穿过，C 自己的 1 号道只画节点到父的斜线（无竖线墨迹，不在 active 内）
        assertThat(rows[1].active_lanes).containsExactly(0).inOrder()
        // A 行：根提交，只画顶边到节点的半段
        assertThat(rows[2].active_lanes).containsExactly(0).inOrder()
    }

    @Test
    fun `merge collapses lanes and flags is_merge`() {
        // R ← A / B，M 合并 A 与 B；log 顺序 M、A、B、R
        val rows = build(
            node("M", "A", "B"),
            node("A", "R"),
            node("B", "R"),
            node("R")
        )

        assertThat(rows).hasSize(4)
        // 只有 M 是合并提交
        assertThat(rows.map { it.is_merge }).containsExactly(true, false, false, false).inOrder()

        // M 行：A 继承 0 号道，B 分叉到 1 号道，两道都向下延续
        assertThat(rows[0].node_lane).isEqualTo(0)
        assertThat(rows[0].active_lanes).containsExactly(0, 1).inOrder()

        // B 行：节点在 1 号道，首父 R 已被 A 预占在 0 号道 → 并入收敛，1 号道在本行终结
        assertThat(rows[2].node_lane).isEqualTo(1)

        // 合并后泳道收敛：B 行双道，R 行只剩 0 号道，活跃泳道总数下降
        assertThat(rows[2].active_lanes).containsExactly(0, 1).inOrder()
        assertThat(rows[3].active_lanes).containsExactly(0).inOrder()
        assertThat(rows[3].active_lanes.size).isLessThan(rows[2].active_lanes.size)
    }

    @Test
    fun `root commit lane is released`() {
        // 孤根：无进出线，泳道 0 不出现在 active_lanes（无线可画），处理后立即回收
        val lone = build(node("a"))
        assertThat(lone).hasSize(1)
        assertThat(lone[0].node_lane).isEqualTo(0)
        assertThat(lone[0].active_lanes).isEmpty()

        // 链尾根提交：只画顶边到节点的半段，之后 0 号道不再被占用
        val chain = build(node("b", "a"), node("a"))
        assertThat(chain[1].active_lanes).containsExactly(0).inOrder()
    }

    @Test
    fun `freed lane is reused by smallest number`() {
        // 分叉(A,B from R) → 合并 M 收敛释放 1 号道 → 后续新分叉 G 复用 1 号道
        val rows = build(
            node("H", "M"),
            node("M", "A", "B"), // 合并：B 占 1 号道
            node("A", "R"),
            node("B", "R"),      // B 的父 R 已被 A 预占 → 1 号道终结回收
            node("G", "Z"),      // 新顶端：最小未占 = 1（复用），而非新开 2
            node("Z", "R"),
            node("R")
        )

        assertThat(rows).hasSize(7)
        // 关键断言：G 落在回收后的最小 lane 1，而不是 2
        assertThat(rows[4].node_lane).isEqualTo(1)
        assertThat(rows[4].active_lanes).containsExactly(0, 1).inOrder()

        // 全程泳道号不超过 1（没有浪费性地新开 2 号道）
        val all_lanes = rows.flatMap { it.active_lanes } + rows.map { it.node_lane }
        assertThat(all_lanes.maxOrNull()).isEqualTo(1)
    }

    @Test
    fun `parents outside loaded window terminate lane without crash`() {
        // 浅克隆边界：c1 的父 ghost 不在窗口内 → 不为其保留 lane，线在 c1 终结
        val rows = build(node("c2", "c1"), node("c1", "ghost"))

        assertThat(rows).hasSize(2)
        assertThat(rows[1].node_lane).isEqualTo(0)
        // 终结行仍画顶边到节点的半段，但没有任何向下延续
        assertThat(rows[1].active_lanes).containsExactly(0).inOrder()

        // 边界提交本身是 HEAD 且双父都在窗口外：不崩溃，is_merge 仍按父数判定，无任何竖线
        val shallow_head = build(node("h", "g1", "g2"))
        assertThat(shallow_head).hasSize(1)
        assertThat(shallow_head[0].is_merge).isTrue()
        assertThat(shallow_head[0].node_lane).isEqualTo(0)
        assertThat(shallow_head[0].active_lanes).isEmpty()
    }

    @Test
    fun `empty input yields empty output`() {
        assertThat(git_graph_builder.build(emptyList())).isEmpty()
    }

    @Test
    fun `thousand commits with merges build under 100ms`() {
        // 1000 个提交（索引小 = 新），约 10% 合并提交（第二父 = i+2），全部父在窗口内
        val commits = ArrayList<git_commit_node>(1000)
        for (i in 0 until 1000) {
            val parents = when {
                i == 999 -> emptyList()                       // 根
                i % 10 == 0 && i + 2 <= 999 -> listOf("c${i + 1}", "c${i + 2}")
                else -> listOf("c${i + 1}")
            }
            commits.add(git_commit_node("c$i", parents))
        }
        val merge_count = commits.count { it.parents.size >= 2 }
        assertThat(merge_count).isAtLeast(90)

        val start = System.currentTimeMillis()
        val rows = git_graph_builder.build(commits)
        val elapsed = System.currentTimeMillis() - start

        assertThat(rows).hasSize(1000)
        assertThat(elapsed).isLessThan(100)
    }

    @Test
    fun `adjacent rows stay visually continuous`() {
        // 固定种子构造 20 提交 DAG（索引小 = 新，父必在窗口内且更旧）
        val rnd = java.util.Random(42)
        val commits = ArrayList<git_commit_node>(20)
        for (i in 0 until 20) {
            val parents = ArrayList<Int>()
            if (i < 19) parents.add(i + 1)                          // 首父：主链
            if (i < 19 && i + 2 <= 19 && rnd.nextDouble() < 0.3) {  // 约 30% 行加第二父
                parents.add(i + 2)
            }
            commits.add(git_commit_node("c$i", parents.map { "c$it" }))
        }
        val rows = git_graph_builder.build(commits)
        assertThat(rows).hasSize(20)

        // 通用约束：升序去重
        for (row in rows) {
            assertThat(row.active_lanes).isEqualTo(row.active_lanes.distinct().sorted())
        }

        // 窗口内 hash 集合，用于判断某父是否在已加载窗口内（浅克隆边界）
        val window = commits.map { it.hash }.toSet()

        for (i in 0 until 19) {
            val current = rows[i].active_lanes.toSet()
            val next = rows[i + 1].active_lanes.toSet()

            // 不变式一（只在节点处终结）：本行活跃、下一行消失的泳道，只能是本行节点所在的泳道。
            // 含义：竖线不会悬空中断，必须终结在某个节点上
            val vanished = current - next
            assertThat(vanished.filter { it != rows[i].node_lane }).isEmpty()

            // 不变式二（只在节点/父处新生）：下一行凭空出现的泳道，只能由下一行提交自己的父分叉产生——
            // 首父继承节点已占道（非新生），其余父各占一条新道（可能是刚回收的号码，那不算凭空出现）。
            // 因此新生数 ≤ max(0, 窗口内父数 - 1) +（节点本身是新顶端时 1 条）
            val in_window_parents = commits[i + 1].parents.count { it in window }
            val node_is_new_tip = rows[i + 1].node_lane !in current
            val newborn_bound = maxOf(0, in_window_parents - 1) + if (node_is_new_tip) 1 else 0
            assertThat((next - current).size).isAtMost(newborn_bound)
        }
    }
}
