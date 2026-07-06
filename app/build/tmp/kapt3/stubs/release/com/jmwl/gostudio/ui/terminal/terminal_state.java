package com.jmwl.gostudio.ui.terminal;

@kotlin.Metadata(mv = {2, 3, 0}, k = 1, xi = 48, d1 = {"\u0000>\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0007\n\u0002\u0010\b\n\u0002\b\u0007\n\u0002\u0018\u0002\n\u0002\b\u0007\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0010\u000b\n\u0002\b\u000b\n\u0002\u0010\u0002\n\u0000\u0018\u00002\u00020\u0001B\u0007\u00a2\u0006\u0004\b\u0002\u0010\u0003J\u0006\u0010.\u001a\u00020/R/\u0010\u0006\u001a\u0004\u0018\u00010\u00052\b\u0010\u0004\u001a\u0004\u0018\u00010\u00058F@FX\u0086\u008e\u0002\u00a2\u0006\u0012\n\u0004\b\u000b\u0010\f\u001a\u0004\b\u0007\u0010\b\"\u0004\b\t\u0010\nR+\u0010\u000e\u001a\u00020\r2\u0006\u0010\u0004\u001a\u00020\r8F@FX\u0086\u008e\u0002\u00a2\u0006\u0012\n\u0004\b\u0013\u0010\u0014\u001a\u0004\b\u000f\u0010\u0010\"\u0004\b\u0011\u0010\u0012R/\u0010\u0016\u001a\u0004\u0018\u00010\u00152\b\u0010\u0004\u001a\u0004\u0018\u00010\u00158F@FX\u0086\u008e\u0002\u00a2\u0006\u0012\n\u0004\b\u001b\u0010\f\u001a\u0004\b\u0017\u0010\u0018\"\u0004\b\u0019\u0010\u001aR\u0017\u0010\u001c\u001a\b\u0012\u0004\u0012\u00020\u001e0\u001d\u00a2\u0006\b\n\u0000\u001a\u0004\b\u001f\u0010 R\u0017\u0010!\u001a\b\u0012\u0004\u0012\u00020\u00150\u001d\u00a2\u0006\b\n\u0000\u001a\u0004\b\"\u0010 R+\u0010$\u001a\u00020#2\u0006\u0010\u0004\u001a\u00020#8F@FX\u0086\u008e\u0002\u00a2\u0006\u0012\n\u0004\b)\u0010\f\u001a\u0004\b%\u0010&\"\u0004\b\'\u0010(R+\u0010*\u001a\u00020#2\u0006\u0010\u0004\u001a\u00020#8F@FX\u0086\u008e\u0002\u00a2\u0006\u0012\n\u0004\b-\u0010\f\u001a\u0004\b+\u0010&\"\u0004\b,\u0010(\u00a8\u00060"}, d2 = {"Lcom/jmwl/gostudio/ui/terminal/terminal_state;", "", "<init>", "()V", "<set-?>", "Lcom/termux/view/TerminalView;", "terminal_view", "getTerminal_view", "()Lcom/termux/view/TerminalView;", "setTerminal_view", "(Lcom/termux/view/TerminalView;)V", "terminal_view$delegate", "Landroidx/compose/runtime/MutableState;", "", "selected_tab_index", "getSelected_tab_index", "()I", "setSelected_tab_index", "(I)V", "selected_tab_index$delegate", "Landroidx/compose/runtime/MutableIntState;", "Lcom/termux/terminal/TerminalSession;", "terminal_session", "getTerminal_session", "()Lcom/termux/terminal/TerminalSession;", "setTerminal_session", "(Lcom/termux/terminal/TerminalSession;)V", "terminal_session$delegate", "terminal_tabs", "Landroidx/compose/runtime/snapshots/SnapshotStateList;", "Lcom/jmwl/gostudio/ui/terminal/terminal_tab;", "getTerminal_tabs", "()Landroidx/compose/runtime/snapshots/SnapshotStateList;", "terminal_sessions", "getTerminal_sessions", "", "ctrl_active", "getCtrl_active", "()Z", "setCtrl_active", "(Z)V", "ctrl_active$delegate", "alt_active", "getAlt_active", "setAlt_active", "alt_active$delegate", "dispose", "", "app_release"})
public final class terminal_state {
    @org.jetbrains.annotations.NotNull()
    private final androidx.compose.runtime.MutableState terminal_view$delegate = null;
    @org.jetbrains.annotations.NotNull()
    private final androidx.compose.runtime.MutableIntState selected_tab_index$delegate = null;
    @org.jetbrains.annotations.NotNull()
    private final androidx.compose.runtime.MutableState terminal_session$delegate = null;
    @org.jetbrains.annotations.NotNull()
    private final androidx.compose.runtime.snapshots.SnapshotStateList<com.jmwl.gostudio.ui.terminal.terminal_tab> terminal_tabs = null;
    @org.jetbrains.annotations.NotNull()
    private final androidx.compose.runtime.snapshots.SnapshotStateList<com.termux.terminal.TerminalSession> terminal_sessions = null;
    @org.jetbrains.annotations.NotNull()
    private final androidx.compose.runtime.MutableState ctrl_active$delegate = null;
    @org.jetbrains.annotations.NotNull()
    private final androidx.compose.runtime.MutableState alt_active$delegate = null;
    
    public terminal_state() {
        super();
    }
    
    @org.jetbrains.annotations.Nullable()
    public final com.termux.view.TerminalView getTerminal_view() {
        return null;
    }
    
    public final void setTerminal_view(@org.jetbrains.annotations.Nullable()
    com.termux.view.TerminalView p0) {
    }
    
    public final int getSelected_tab_index() {
        return 0;
    }
    
    public final void setSelected_tab_index(int p0) {
    }
    
    @org.jetbrains.annotations.Nullable()
    public final com.termux.terminal.TerminalSession getTerminal_session() {
        return null;
    }
    
    public final void setTerminal_session(@org.jetbrains.annotations.Nullable()
    com.termux.terminal.TerminalSession p0) {
    }
    
    @org.jetbrains.annotations.NotNull()
    public final androidx.compose.runtime.snapshots.SnapshotStateList<com.jmwl.gostudio.ui.terminal.terminal_tab> getTerminal_tabs() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final androidx.compose.runtime.snapshots.SnapshotStateList<com.termux.terminal.TerminalSession> getTerminal_sessions() {
        return null;
    }
    
    public final boolean getCtrl_active() {
        return false;
    }
    
    public final void setCtrl_active(boolean p0) {
    }
    
    public final boolean getAlt_active() {
        return false;
    }
    
    public final void setAlt_active(boolean p0) {
    }
    
    public final void dispose() {
    }
}