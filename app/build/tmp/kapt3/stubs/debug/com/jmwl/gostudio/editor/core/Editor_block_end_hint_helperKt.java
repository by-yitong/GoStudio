package com.jmwl.gostudio.editor.core;

@kotlin.Metadata(mv = {2, 3, 0}, k = 2, xi = 48, d1 = {"\u0000D\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u000b\n\u0002\b\u0003\n\u0002\u0010\b\n\u0002\b\u000b\n\u0002\u0010\t\n\u0002\b\u0003\u001a\u0018\u0010\u0000\u001a\u0004\u0018\u00010\u00012\u0006\u0010\u0002\u001a\u00020\u0003H\u0080@\u00a2\u0006\u0002\u0010\u0004\u001a\u001e\u0010\u0005\u001a\n\u0012\u0004\u0012\u00020\u0007\u0018\u00010\u00062\u0006\u0010\u0002\u001a\u00020\u0003H\u0082@\u00a2\u0006\u0002\u0010\u0004\u001a$\u0010\b\u001a\u00020\u00012\f\u0010\t\u001a\b\u0012\u0004\u0012\u00020\n0\u00062\f\u0010\u000b\u001a\b\u0012\u0004\u0012\u00020\u00070\u0006H\u0002\u001a\u001c\u0010\f\u001a\u0004\u0018\u00010\r*\u00020\u00072\f\u0010\t\u001a\b\u0012\u0004\u0012\u00020\n0\u0006H\u0002\u001a\u0018\u0010\u000e\u001a\b\u0012\u0004\u0012\u00020\r0\u0006*\b\u0012\u0004\u0012\u00020\r0\u0006H\u0002\u001a\u0014\u0010\u000f\u001a\u00020\u0010*\u00020\u00072\u0006\u0010\u0011\u001a\u00020\u0007H\u0002\u001a&\u0010\u0012\u001a\u00020\u00102\f\u0010\t\u001a\b\u0012\u0004\u0012\u00020\n0\u00062\u0006\u0010\u0013\u001a\u00020\u00142\u0006\u0010\u0015\u001a\u00020\u0014H\u0002\u001a\u0018\u0010\u0016\u001a\u00020\u00142\u0006\u0010\u0017\u001a\u00020\n2\u0006\u0010\u0018\u001a\u00020\u0014H\u0002\u001a&\u0010\u0019\u001a\u00020\n2\f\u0010\t\u001a\b\u0012\u0004\u0012\u00020\n0\u00062\u0006\u0010\u001a\u001a\u00020\u00142\u0006\u0010\u001b\u001a\u00020\u0014H\u0002\u001a&\u0010\u001c\u001a\u00020\n2\f\u0010\t\u001a\b\u0012\u0004\u0012\u00020\n0\u00062\u0006\u0010\u001a\u001a\u00020\u00142\u0006\u0010\u001b\u001a\u00020\u0014H\u0002\u001a\u0010\u0010\u001d\u001a\u00020\n2\u0006\u0010\u001e\u001a\u00020\nH\u0002\"\u000e\u0010\u001f\u001a\u00020 X\u0082T\u00a2\u0006\u0002\n\u0000\"\u000e\u0010!\u001a\u00020\u0014X\u0082T\u00a2\u0006\u0002\n\u0000\"\u000e\u0010\"\u001a\u00020\u0014X\u0082T\u00a2\u0006\u0002\n\u0000\u00a8\u0006#"}, d2 = {"build_editor_block_end_hints", "Lio/github/rosemoe/sora/lang/styling/inlayHint/InlayHintsContainer;", "editor", "Lio/github/rosemoe/sora/widget/CodeEditor;", "(Lio/github/rosemoe/sora/widget/CodeEditor;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "wait_for_sora_code_blocks", "", "Lio/github/rosemoe/sora/lang/styling/CodeBlock;", "build_block_end_hints_from_sora_blocks", "lines", "", "blocks", "to_block_end_hint_candidate", "Lcom/jmwl/gostudio/editor/core/block_end_hint_candidate;", "filter_outer_block_end_hints", "contains_code_block", "", "block", "is_c_comment_position", "target_line", "", "target_column", "block_end_hint_header_end_column", "line", "fallback_column", "block_end_hint_label", "line_index", "header_end_column", "block_end_hint_header", "normalize_block_end_hint_header", "header", "block_end_hint_sora_block_wait_delay_ms", "", "block_end_hint_sora_block_wait_attempts", "max_block_end_hint_header_lookback_lines", "app_debug"})
public final class Editor_block_end_hint_helperKt {
    private static final long block_end_hint_sora_block_wait_delay_ms = 50L;
    private static final int block_end_hint_sora_block_wait_attempts = 40;
    private static final int max_block_end_hint_header_lookback_lines = 6;
    
    @org.jetbrains.annotations.Nullable()
    public static final java.lang.Object build_editor_block_end_hints(@org.jetbrains.annotations.NotNull()
    io.github.rosemoe.sora.widget.CodeEditor editor, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super io.github.rosemoe.sora.lang.styling.inlayHint.InlayHintsContainer> $completion) {
        return null;
    }
    
    private static final java.lang.Object wait_for_sora_code_blocks(io.github.rosemoe.sora.widget.CodeEditor editor, kotlin.coroutines.Continuation<? super java.util.List<? extends io.github.rosemoe.sora.lang.styling.CodeBlock>> $completion) {
        return null;
    }
    
    private static final io.github.rosemoe.sora.lang.styling.inlayHint.InlayHintsContainer build_block_end_hints_from_sora_blocks(java.util.List<java.lang.String> lines, java.util.List<? extends io.github.rosemoe.sora.lang.styling.CodeBlock> blocks) {
        return null;
    }
    
    private static final com.jmwl.gostudio.editor.core.block_end_hint_candidate to_block_end_hint_candidate(io.github.rosemoe.sora.lang.styling.CodeBlock $this$to_block_end_hint_candidate, java.util.List<java.lang.String> lines) {
        return null;
    }
    
    private static final java.util.List<com.jmwl.gostudio.editor.core.block_end_hint_candidate> filter_outer_block_end_hints(java.util.List<com.jmwl.gostudio.editor.core.block_end_hint_candidate> $this$filter_outer_block_end_hints) {
        return null;
    }
    
    private static final boolean contains_code_block(io.github.rosemoe.sora.lang.styling.CodeBlock $this$contains_code_block, io.github.rosemoe.sora.lang.styling.CodeBlock block) {
        return false;
    }
    
    private static final boolean is_c_comment_position(java.util.List<java.lang.String> lines, int target_line, int target_column) {
        return false;
    }
    
    private static final int block_end_hint_header_end_column(java.lang.String line, int fallback_column) {
        return 0;
    }
    
    private static final java.lang.String block_end_hint_label(java.util.List<java.lang.String> lines, int line_index, int header_end_column) {
        return null;
    }
    
    private static final java.lang.String block_end_hint_header(java.util.List<java.lang.String> lines, int line_index, int header_end_column) {
        return null;
    }
    
    private static final java.lang.String normalize_block_end_hint_header(java.lang.String header) {
        return null;
    }
}