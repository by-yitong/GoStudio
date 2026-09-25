package com.jmwl.gostudio.tools

import com.google.common.truth.Truth.assertThat
import com.jmwl.gostudio.toolchain.git_ref_entry
import com.jmwl.gostudio.toolchain.git_ref_kind
import com.jmwl.gostudio.ui.screens.editor.build_ref_tree
import com.jmwl.gostudio.ui.screens.editor.git_ref_tree_node
import org.junit.Test

/**
 * build_ref_tree 层级树构建回归：
 * 覆盖平铺分支、两级嵌套、三段深路径、本地/远程同名共存、空列表、
 * 排序稳定性（文件夹在前 / 字典序、与输入顺序无关）。
 */
class git_ref_tree_test {

    private fun local(name: String, head: Boolean = false) = git_ref_entry(
        kind = git_ref_kind.LOCAL,
        short_name = name,
        full_ref = "refs/heads/$name",
        hash = "l-$name",
        is_head = head,
        upstream = "",
        ahead = 0,
        behind = 0
    )

    private fun remote(name: String) = git_ref_entry(
        kind = git_ref_kind.REMOTE,
        short_name = name,
        full_ref = "refs/remotes/$name",
        hash = "r-$name",
        is_head = false,
        upstream = "",
        ahead = 0,
        behind = 0
    )

    private fun tag(name: String) = git_ref_entry(
        kind = git_ref_kind.TAG,
        short_name = name,
        full_ref = "refs/tags/$name",
        hash = "t-$name",
        is_head = false,
        upstream = "",
        ahead = 0,
        behind = 0
    )

    private fun collect_leaves(nodes: List<git_ref_tree_node>): List<git_ref_entry> =
        nodes.flatMap { node ->
            if (node.is_leaf) listOf(node.ref!!) else collect_leaves(node.children)
        }

    @Test
    fun `flat refs without slash build single level leaves`() {
        val refs = listOf(local("main", head = true), local("dev"), tag("v1.0"))
        val tree = build_ref_tree(refs)

        assertThat(tree).hasSize(3)
        assertThat(tree.map { it.name }).containsExactly("dev", "main", "v1.0").inOrder()
        tree.forEach { node ->
            assertThat(node.is_leaf).isTrue()
            assertThat(node.children).isEmpty()
            assertThat(node.ref).isNotNull()
            assertThat(node.full_path).isEqualTo(node.name)
        }
        assertThat(tree[1].ref?.is_head).isTrue()
        assertThat(collect_leaves(tree).map { it.hash })
            .containsExactly("l-dev", "l-main", "t-v1.0")
    }

    @Test
    fun `two segment refs group under shared folder`() {
        val refs = listOf(
            local("feature/login"),
            local("feature/signup"),
            local("main")
        )
        val tree = build_ref_tree(refs)

        assertThat(tree).hasSize(2)
        val feature = tree[0]
        assertThat(feature.name).isEqualTo("feature")
        assertThat(feature.full_path).isEqualTo("feature")
        assertThat(feature.is_leaf).isFalse()
        assertThat(feature.ref).isNull()
        assertThat(feature.children.map { it.name }).containsExactly("login", "signup").inOrder()
        feature.children.forEach { child ->
            assertThat(child.is_leaf).isTrue()
            assertThat(child.ref?.kind).isEqualTo(git_ref_kind.LOCAL)
        }
        assertThat(feature.children[0].full_path).isEqualTo("feature/login")

        assertThat(tree[1].name).isEqualTo("main")
        assertThat(tree[1].is_leaf).isTrue()
        assertThat(collect_leaves(tree)).hasSize(3)
    }

    @Test
    fun `three segment path nests two folder levels`() {
        val refs = listOf(local("bugfix/ui/crash"))
        val tree = build_ref_tree(refs)

        assertThat(tree).hasSize(1)
        val bugfix = tree[0]
        assertThat(bugfix.name).isEqualTo("bugfix")
        assertThat(bugfix.full_path).isEqualTo("bugfix")
        assertThat(bugfix.is_leaf).isFalse()

        val ui = bugfix.children.single()
        assertThat(ui.name).isEqualTo("ui")
        assertThat(ui.full_path).isEqualTo("bugfix/ui")
        assertThat(ui.is_leaf).isFalse()

        val crash = ui.children.single()
        assertThat(crash.name).isEqualTo("crash")
        assertThat(crash.full_path).isEqualTo("bugfix/ui/crash")
        assertThat(crash.is_leaf).isTrue()
        assertThat(crash.ref).isEqualTo(refs[0])
        assertThat(crash.children).isEmpty()
    }

    @Test
    fun `local and remote with same base name coexist`() {
        val refs = listOf(
            local("dev"),
            remote("origin/dev"),
            local("feature/login"),
            remote("origin/feature/login")
        )
        val tree = build_ref_tree(refs)

        // 文件夹在前：feature、origin，其后叶子 dev
        assertThat(tree.map { it.name }).containsExactly("feature", "origin", "dev").inOrder()
        assertThat(tree[2].is_leaf).isTrue()
        assertThat(tree[2].ref?.kind).isEqualTo(git_ref_kind.LOCAL)

        val local_feature = tree[0]
        assertThat(local_feature.children.single().name).isEqualTo("login")
        assertThat(local_feature.children.single().ref?.kind).isEqualTo(git_ref_kind.LOCAL)
        assertThat(local_feature.children.single().full_path).isEqualTo("feature/login")

        val origin = tree[1]
        // origin 下既有 feature 文件夹（文件夹在前），又有直接叶子 dev
        assertThat(origin.children.map { it.name }).containsExactly("feature", "dev").inOrder()
        val remote_feature = origin.children[0]
        assertThat(remote_feature.name).isEqualTo("feature")
        assertThat(remote_feature.is_leaf).isFalse()
        val remote_login = remote_feature.children.single()
        assertThat(remote_login.name).isEqualTo("login")
        assertThat(remote_login.ref?.kind).isEqualTo(git_ref_kind.REMOTE)
        assertThat(remote_login.full_path).isEqualTo("origin/feature/login")
        val origin_dev = origin.children[1]
        assertThat(origin_dev.is_leaf).isTrue()
        assertThat(origin_dev.ref?.kind).isEqualTo(git_ref_kind.REMOTE)
        assertThat(origin_dev.full_path).isEqualTo("origin/dev")

        // 四条引用全部保留、不丢不重
        val leaves = collect_leaves(tree)
        assertThat(leaves).hasSize(4)
        assertThat(leaves.map { it.full_ref }).containsNoDuplicates()
    }

    @Test
    fun `empty refs return empty tree`() {
        assertThat(build_ref_tree(emptyList())).isEmpty()
    }

    @Test
    fun `folders sort before leaves and output is stable across input order`() {
        val refs = listOf(
            local("zebra"),
            local("app"),
            local("feat/y"),
            local("alpha/beta"),
            local("feat/x")
        )
        val tree = build_ref_tree(refs)

        // 文件夹在前（alpha、feat），叶子按字典序（app、zebra）
        assertThat(tree.map { it.name }).containsExactly("alpha", "feat", "app", "zebra").inOrder()
        assertThat(tree[0].is_leaf).isFalse()
        assertThat(tree[1].is_leaf).isFalse()
        assertThat(tree[2].is_leaf).isTrue()
        assertThat(tree[3].is_leaf).isTrue()

        val feat = tree[1]
        assertThat(feat.children.map { it.name }).containsExactly("x", "y").inOrder()

        // 排序与输入顺序无关，重复构建结果稳定
        val shuffled = build_ref_tree(refs.reversed())
        assertThat(shuffled).isEqualTo(tree)
        assertThat(build_ref_tree(refs)).isEqualTo(tree)
    }
}
