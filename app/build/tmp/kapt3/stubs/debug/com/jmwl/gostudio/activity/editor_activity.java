package com.jmwl.gostudio.activity;

@kotlin.Metadata(mv = {2, 3, 0}, k = 1, xi = 48, d1 = {"\u0000\u00ec\u0001\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000b\n\u0000\n\u0002\u0010\u000e\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010#\n\u0000\n\u0002\u0010%\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\t\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\b\b\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\b\u0018\n\u0002\u0018\u0002\n\u0002\b\u000b\n\u0002\u0018\u0002\n\u0002\b\u0007\n\u0002\u0010\b\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0015\n\u0002\u0010\"\n\u0002\u0018\u0002\n\u0002\b\n\n\u0002\u0018\u0002\n\u0002\b$\n\u0002\u0018\u0002\n\u0002\b\u0012\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0002\b\u0006\u0018\u0000 \u00df\u00012\u00020\u0001:\u0004\u00de\u0001\u00df\u0001B\u0007\u00a2\u0006\u0004\b\u0002\u0010\u0003J\u0012\u0010\'\u001a\u00020(2\b\u0010)\u001a\u0004\u0018\u00010*H\u0014J\b\u0010+\u001a\u00020(H\u0014J\b\u0010,\u001a\u00020(H\u0002J\b\u0010-\u001a\u00020(H\u0003J\b\u0010.\u001a\u00020$H\u0002J\b\u0010/\u001a\u00020\u0007H\u0002J\u0012\u00100\u001a\u00020\u00072\b\u00101\u001a\u0004\u0018\u00010\u0011H\u0002J\u0016\u00102\u001a\u00020(2\f\u00103\u001a\b\u0012\u0004\u0012\u00020(04H\u0002J\b\u00105\u001a\u00020(H\u0002J\u0010\u00106\u001a\u00020(2\u0006\u00107\u001a\u00020\u0007H\u0002J\u0010\u00108\u001a\u00020(2\u0006\u00109\u001a\u00020:H\u0002J\u0010\u0010;\u001a\u00020(2\u0006\u00109\u001a\u00020:H\u0002J\u000e\u0010<\u001a\b\u0012\u0004\u0012\u00020\u00070\u001fH\u0002J\b\u0010=\u001a\u00020(H\u0002J\u0018\u0010>\u001a\u00020(2\u0006\u0010?\u001a\u00020\u00072\u0006\u00109\u001a\u00020:H\u0002J\b\u0010@\u001a\u00020(H\u0002J\u001e\u0010A\u001a\u00020(2\u0006\u0010B\u001a\u00020C2\f\u0010D\u001a\b\u0012\u0004\u0012\u00020(04H\u0002J\b\u0010E\u001a\u00020(H\u0002J\b\u0010F\u001a\u00020(H\u0002J\u0010\u0010G\u001a\u00020(2\u0006\u0010H\u001a\u00020IH\u0002J\u0010\u0010J\u001a\u00020(2\u0006\u00101\u001a\u00020\u0011H\u0002J\u0010\u0010K\u001a\u00020(2\u0006\u00101\u001a\u00020\u0011H\u0002J\u0010\u0010L\u001a\u00020(2\u0006\u00101\u001a\u00020\u0011H\u0002J\u0010\u0010M\u001a\u00020(2\u0006\u0010N\u001a\u00020\u0011H\u0002J\b\u0010O\u001a\u00020(H\u0002J\b\u0010P\u001a\u00020(H\u0002J\b\u0010Q\u001a\u00020(H\u0002J\b\u0010R\u001a\u00020(H\u0002J$\u0010S\u001a\u00020(2\b\b\u0002\u0010T\u001a\u00020\u000f2\u0010\b\u0002\u0010D\u001a\n\u0012\u0004\u0012\u00020(\u0018\u000104H\u0002J\b\u0010U\u001a\u00020(H\u0002J\b\u0010V\u001a\u00020(H\u0002J\b\u0010W\u001a\u00020(H\u0002J\b\u0010X\u001a\u00020(H\u0002J\b\u0010Y\u001a\u00020(H\u0002J*\u0010X\u001a\u00020(2\u0006\u0010Z\u001a\u00020\u000f2\u0006\u0010T\u001a\u00020\u000f2\u0010\b\u0002\u0010[\u001a\n\u0012\u0004\u0012\u00020(\u0018\u000104H\u0002J2\u0010\\\u001a\u00020\u00112\u0006\u0010]\u001a\u00020\u00112\u0006\u0010^\u001a\u00020\u00112\u0006\u0010_\u001a\u00020\u00112\b\u0010`\u001a\u0004\u0018\u00010\u00112\u0006\u0010a\u001a\u00020bH\u0002J\u0010\u0010c\u001a\u00020b2\u0006\u0010\u0004\u001a\u00020\u0005H\u0002J\u0010\u0010d\u001a\u00020b2\u0006\u0010\u0004\u001a\u00020\u0005H\u0002J\u0012\u0010e\u001a\u0004\u0018\u00010\u00112\u0006\u0010f\u001a\u00020\u0011H\u0002J\u0012\u0010g\u001a\u0004\u0018\u00010\u00112\u0006\u0010f\u001a\u00020\u0011H\u0002J\u0010\u0010h\u001a\u00020\u000f2\u0006\u0010f\u001a\u00020\u0011H\u0002J\u0010\u0010i\u001a\u00020\u00112\u0006\u0010j\u001a\u00020\u0011H\u0002J\u0012\u0010k\u001a\u0004\u0018\u00010\u00112\u0006\u0010^\u001a\u00020\u0011H\u0002J\u0010\u0010l\u001a\u00020\u000f2\u0006\u0010^\u001a\u00020\u0011H\u0002J\u0010\u0010m\u001a\u00020n2\u0006\u0010o\u001a\u00020\u0011H\u0002J\u0010\u0010p\u001a\u00020\u00112\u0006\u0010j\u001a\u00020\u0011H\u0002J\b\u0010q\u001a\u00020(H\u0002J\b\u0010r\u001a\u00020(H\u0002J\b\u0010s\u001a\u00020(H\u0002J\u0010\u0010t\u001a\u00020(2\u0006\u00101\u001a\u00020\u0011H\u0002J \u0010u\u001a\u00020(2\u0006\u00101\u001a\u00020\u00112\u0006\u0010o\u001a\u00020v2\u0006\u0010w\u001a\u00020vH\u0002J\u0010\u0010x\u001a\u00020y2\u0006\u0010z\u001a\u00020{H\u0002J\u0016\u0010|\u001a\u00020y2\u0006\u0010z\u001a\u00020{H\u0082@\u00a2\u0006\u0002\u0010}J\u0018\u0010~\u001a\u00020(2\u0006\u0010o\u001a\u00020v2\u0006\u0010w\u001a\u00020vH\u0002J\u000f\u0010\u007f\u001a\u00020(H\u0082@\u00a2\u0006\u0003\u0010\u0080\u0001J\t\u0010\u0081\u0001\u001a\u00020(H\u0002J\u0018\u0010\u0082\u0001\u001a\u00020\u000f2\u0006\u0010T\u001a\u00020\u000fH\u0082@\u00a2\u0006\u0003\u0010\u0083\u0001J\u0018\u0010\u0084\u0001\u001a\u00020\u000f2\u0006\u0010T\u001a\u00020\u000fH\u0082@\u00a2\u0006\u0003\u0010\u0083\u0001J\u0011\u0010\u0085\u0001\u001a\u00020(2\u0006\u00101\u001a\u00020\u0011H\u0002J\u0011\u0010\u0086\u0001\u001a\u00020(2\u0006\u00101\u001a\u00020\u0011H\u0002J\u0011\u0010\u0087\u0001\u001a\u00020(2\u0006\u00101\u001a\u00020\u0011H\u0002J\u0011\u0010\u0088\u0001\u001a\u00020(2\u0006\u0010N\u001a\u00020\u0011H\u0002J\t\u0010\u0089\u0001\u001a\u00020(H\u0002J\t\u0010\u008a\u0001\u001a\u00020(H\u0002J\u001d\u0010\u008b\u0001\u001a\u00020(2\u0007\u0010\u008c\u0001\u001a\u00020v2\t\b\u0002\u0010\u008d\u0001\u001a\u00020\u000fH\u0002J\u001b\u0010\u008e\u0001\u001a\u00020(2\u0007\u0010\u008c\u0001\u001a\u00020v2\u0007\u0010\u008f\u0001\u001a\u00020yH\u0002J\u0019\u0010\u0090\u0001\u001a\n\u0012\u0005\u0012\u00030\u0092\u00010\u0091\u00012\u0006\u00109\u001a\u00020:H\u0002J\t\u0010\u0093\u0001\u001a\u00020(H\u0002J\t\u0010\u0094\u0001\u001a\u00020(H\u0002J\u0012\u0010\u0095\u0001\u001a\u00020(2\u0007\u0010\u008f\u0001\u001a\u00020yH\u0002J\u0013\u0010\u0096\u0001\u001a\u0004\u0018\u00010\u00112\u0006\u0010o\u001a\u00020\u0011H\u0002J\t\u0010\u0097\u0001\u001a\u00020\u0011H\u0002J\u001b\u0010\u0098\u0001\u001a\u00020(2\u0007\u0010\u0099\u0001\u001a\u00020\u00052\u0007\u0010\u009a\u0001\u001a\u00020\u0011H\u0002J\u001c\u0010\u009b\u0001\u001a\u00020(2\u0007\u0010\u0099\u0001\u001a\u00020\u00052\b\u0010\u009c\u0001\u001a\u00030\u009d\u0001H\u0002J\u0012\u0010\u009e\u0001\u001a\u00020(2\u0007\u0010\u008f\u0001\u001a\u00020yH\u0002J\t\u0010\u009f\u0001\u001a\u00020(H\u0002J\t\u0010\u00a0\u0001\u001a\u00020(H\u0002J\u000b\u0010\u00a1\u0001\u001a\u0004\u0018\u00010yH\u0002J\u0012\u0010\u00a2\u0001\u001a\u00020(2\u0007\u0010\u008f\u0001\u001a\u00020yH\u0002J\u0013\u0010\u00a3\u0001\u001a\u00020v2\b\u00101\u001a\u0004\u0018\u00010\u0011H\u0002J\u001c\u0010\u00a4\u0001\u001a\u00020(2\u0011\b\u0002\u0010\u00a5\u0001\u001a\n\u0012\u0004\u0012\u00020(\u0018\u000104H\u0002J\u0012\u0010\u00a6\u0001\u001a\u00020(2\u0007\u0010\u00a7\u0001\u001a\u00020\u0011H\u0002J)\u0010\u00a8\u0001\u001a\u00020(2\u000b\b\u0002\u0010\u00a9\u0001\u001a\u0004\u0018\u00010\u00112\u0011\b\u0002\u0010\u00a5\u0001\u001a\n\u0012\u0004\u0012\u00020(\u0018\u000104H\u0002J0\u0010\u00aa\u0001\u001a\u00020(2\u0007\u0010\u00a7\u0001\u001a\u00020\u00112\t\b\u0002\u0010\u00ab\u0001\u001a\u00020\u000f2\u0011\b\u0002\u0010\u00a5\u0001\u001a\n\u0012\u0004\u0012\u00020(\u0018\u000104H\u0002J\u001b\u0010\u00ac\u0001\u001a\u00020(2\u0007\u0010\u00ad\u0001\u001a\u00020\u00112\u0007\u0010\u00ae\u0001\u001a\u00020\u0011H\u0002J\u001b\u0010\u00af\u0001\u001a\u00020(2\u0007\u0010\u00ad\u0001\u001a\u00020\u00112\u0007\u0010\u00ae\u0001\u001a\u00020\u0011H\u0002J$\u0010\u00b0\u0001\u001a\u00020(2\u0007\u0010\u00ad\u0001\u001a\u00020\u00112\u0007\u0010\u00ae\u0001\u001a\u00020\u00112\u0007\u0010\u00b1\u0001\u001a\u00020\u000fH\u0002J\u001b\u0010\u00b2\u0001\u001a\u00020(2\u0007\u0010\u00a7\u0001\u001a\u00020\u00112\u0007\u0010\u00b3\u0001\u001a\u00020\u0011H\u0002J\u0012\u0010\u00b4\u0001\u001a\u00020(2\u0007\u0010\u00a7\u0001\u001a\u00020\u0011H\u0002J\u001b\u0010\u00b5\u0001\u001a\u00020(2\u0007\u0010\u00b6\u0001\u001a\u00020\u00112\u0007\u0010\u00b7\u0001\u001a\u00020\u0011H\u0002J\u0012\u0010\u00b8\u0001\u001a\u00020(2\u0007\u0010\u00a7\u0001\u001a\u00020\u0011H\u0002J\u001b\u0010\u00b9\u0001\u001a\u00020(2\u0007\u0010\u00b6\u0001\u001a\u00020\u00112\u0007\u0010\u00b7\u0001\u001a\u00020\u0011H\u0002J\u0012\u0010\u00ba\u0001\u001a\u00020(2\u0007\u0010\u00bb\u0001\u001a\u00020\u0011H\u0002J\t\u0010\u00bc\u0001\u001a\u00020(H\u0002J\t\u0010\u00bd\u0001\u001a\u00020(H\u0002J\t\u0010\u00be\u0001\u001a\u00020(H\u0002J\u0011\u0010\u00bf\u0001\u001a\u0004\u0018\u00010\u000fH\u0002\u00a2\u0006\u0003\u0010\u00c0\u0001J\f\u0010\u00c1\u0001\u001a\u0005\u0018\u00010\u00c2\u0001H\u0002J\t\u0010\u00c3\u0001\u001a\u00020(H\u0002J\u0012\u0010\u00c4\u0001\u001a\u00020(2\u0007\u0010\u00c5\u0001\u001a\u00020\u0011H\u0002J-\u0010\u00c6\u0001\u001a\u00020\u000f2\u0007\u0010\u00c7\u0001\u001a\u00020\u00112\u0007\u0010\u00c8\u0001\u001a\u00020\u000f2\u0007\u0010\u00c9\u0001\u001a\u00020\u000f2\u0007\u0010\u00ca\u0001\u001a\u00020\u000fH\u0002J\u0012\u0010\u00cb\u0001\u001a\u00020(2\u0007\u0010\u00cc\u0001\u001a\u00020\u000fH\u0002J\u0012\u0010\u00cd\u0001\u001a\u00020(2\u0007\u0010\u00ce\u0001\u001a\u00020\u0011H\u0002J\u0012\u0010\u00cf\u0001\u001a\u00020(2\u0007\u0010\u00ce\u0001\u001a\u00020\u0011H\u0002J\t\u0010\u00d0\u0001\u001a\u00020(H\u0002J\u0019\u0010\u00d1\u0001\u001a\u00020\u00072\u0007\u0010\u008f\u0001\u001a\u00020yH\u0082@\u00a2\u0006\u0003\u0010\u00d2\u0001J\u0012\u0010\u00d3\u0001\u001a\u00020\u00072\u0007\u0010\u008f\u0001\u001a\u00020yH\u0002J\u0012\u0010\u00d4\u0001\u001a\u00020(2\u0007\u0010f\u001a\u00030\u00d5\u0001H\u0002J\u0011\u0010\u00d6\u0001\u001a\u00020(2\u0006\u0010f\u001a\u00020\u0011H\u0002J\t\u0010\u00d7\u0001\u001a\u00020(H\u0002J\u0011\u0010\u00d8\u0001\u001a\u00020(2\u0006\u00101\u001a\u00020\u0011H\u0002J\u0016\u0010\u00d9\u0001\u001a\u0005\u0018\u00010\u00da\u00012\b\b\u0002\u0010?\u001a\u00020\u0007H\u0002J\u0012\u0010\u00db\u0001\u001a\u00030\u00da\u00012\u0006\u00101\u001a\u00020\u0011H\u0002J\t\u0010\u00dc\u0001\u001a\u00020(H\u0002J\t\u0010\u00dd\u0001\u001a\u00020(H\u0002R\u000e\u0010\u0004\u001a\u00020\u0005X\u0082.\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0006\u001a\u00020\u0007X\u0082.\u00a2\u0006\u0002\n\u0000R\u000e\u0010\b\u001a\u00020\tX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\n\u001a\u00020\u000bX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\f\u001a\u00020\rX\u0082.\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u000e\u001a\u00020\u000fX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u0010\u0010\u0010\u001a\u0004\u0018\u00010\u0011X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u0010\u0010\u0012\u001a\u0004\u0018\u00010\u0013X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u0010\u0010\u0014\u001a\u0004\u0018\u00010\u0013X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u0010\u0010\u0015\u001a\u0004\u0018\u00010\u0013X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u0010\u0010\u0016\u001a\u0004\u0018\u00010\u0013X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0017\u001a\u00020\u000fX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u0010\u0010\u0018\u001a\u0004\u0018\u00010\u0019X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u0010\u0010\u001a\u001a\u0004\u0018\u00010\u0013X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u0014\u0010\u001b\u001a\b\u0012\u0004\u0012\u00020\u00110\u001cX\u0082\u0004\u00a2\u0006\u0002\n\u0000R \u0010\u001d\u001a\u0014\u0012\u0004\u0012\u00020\u0011\u0012\n\u0012\b\u0012\u0004\u0012\u00020 0\u001f0\u001eX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010!\u001a\u00020\"X\u0082.\u00a2\u0006\u0002\n\u0000R\u000e\u0010#\u001a\u00020$X\u0082.\u00a2\u0006\u0002\n\u0000R\u0014\u0010%\u001a\b\u0012\u0004\u0012\u00020\u00110&X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u00e0\u0001"}, d2 = {"Lcom/jmwl/gostudio/activity/editor_activity;", "Landroidx/activity/ComponentActivity;", "<init>", "()V", "project_dir", "Ljava/io/File;", "editor", "Lio/github/rosemoe/sora/widget/CodeEditor;", "state", "Lcom/jmwl/gostudio/editor/session/editor_activity_state;", "output_panel_state", "Lcom/jmwl/gostudio/ui/screens/editor/editor_output_panel_state;", "detected_project_info", "Lcom/jmwl/gostudio/project/detected_project;", "applying_editor_content", "", "current_textmate_scope", "", "block_hint_job", "Lkotlinx/coroutines/Job;", "cmake_configure_job", "cmake_build_job", "file_tree_job", "textmate_prewarm_started", "clangd_project", "Lcom/jmwl/gostudio/lsp/clangd/clangd_lsp_project;", "clangd_connect_job", "clangd_skipped_files", "", "file_tree_children_cache", "", "", "Lcom/jmwl/gostudio/editor/model/editor_file_node;", "search_controller", "Lcom/jmwl/gostudio/editor/core/editor_search_controller;", "tab_lifecycle", "Lcom/jmwl/gostudio/activity/editor_tab_lifecycle;", "import_editor_font_launcher", "Landroidx/activity/result/ActivityResultLauncher;", "onCreate", "", "savedInstanceState", "Landroid/os/Bundle;", "onDestroy", "append_detected_project_log", "editor_activity_content", "create_tab_lifecycle", "create_code_editor", "create_tab_editor", "file_path", "with_applying_editor_content", "action", "Lkotlin/Function0;", "handle_editor_content_changed", "handle_editor_selection_changed", "changed_editor", "update_editor_settings", "settings", "Lcom/jmwl/gostudio/editor/model/editor_settings_state;", "apply_editor_settings", "open_editors", "apply_colors_to_open_editors", "apply_current_editor_behavior_settings", "target", "request_import_editor_font", "apply_project_config", "config", "Lcom/jmwl/gostudio/project/project_ide_config;", "on_saved", "initialize_project", "prewarm_textmate_languages", "import_editor_font_from_uri", "uri", "Landroid/net/Uri;", "request_open_file", "request_select_tab", "request_close_tab", "request_close_other_tabs", "keep_file_path", "request_close_all_tabs", "request_close_editor", "confirm_close_editor", "close_editor_after_confirmation", "request_save_file", "show_toast", "format_current_file", "handle_build_button_click", "build_cmake_project", "configure_cmake_project", "configure_cmake_project_if_needed", "clean_build", "on_success", "create_cmake_configure_command", "source_dir", "build_dir", "cmake_toolchain_file", "existing_generator", "android_config", "Lcom/jmwl/gostudio/activity/editor_activity$cmake_android_config;", "project_cmake_config", "infer_cmake_android_config", "infer_cmake_android_abi", "content", "infer_cmake_android_platform", "uses_android_vulkan", "enabled_cmake_args", "value", "read_existing_cmake_generator", "reset_cmake_build_dir", "output_level_for_cmake_line", "Lcom/jmwl/gostudio/ui/screens/editor/editor_output_line_level;", "line", "shell_quote", "save_pending_action", "discard_pending_action", "run_pending_action", "open_file", "open_file_at", "", "column", "create_open_tab", "Lcom/jmwl/gostudio/editor/session/editor_open_tab;", "loaded_file", "Lcom/jmwl/gostudio/editor/core/editor_loaded_file;", "open_loaded_file_tab", "(Lcom/jmwl/gostudio/editor/core/editor_loaded_file;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "move_cursor_to", "restore_pinned_tabs", "(Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "save_pinned_tabs", "save_dirty_open_files", "(ZLkotlin/coroutines/Continuation;)Ljava/lang/Object;", "save_current_file", "configure_cmake_after_cmakelists_save", "toggle_pin_tab", "close_tab", "close_other_tabs", "close_all_tabs", "reorder_tabs_keep_active", "attach_editor_tab", "index", "capture_current", "activate_editor_tab", "tab", "disabled_clangd_features", "", "Lio/github/rosemoe/sora/lsp/client/languageserver/LspFeature;", "reset_clangd_project", "restore_editor_languages_from_clangd", "connect_clangd_if_needed", "clean_clangd_log_line", "configured_ndk_clang_prefix", "log_clangd_skip_once", "file", "reason", "log_clangd_status", "status", "Lio/github/rosemoe/sora/lsp/editor/LspEditorStatus;", "restore_editor_selection", "reset_editor_state", "capture_active_tab_state", "active_tab", "release_tab_editor", "find_tab_index", "reload_file_tree", "on_complete", "toggle_directory", "path", "refresh_file_tree", "directory_path", "load_file_tree_directory_if_needed", "force", "create_project_file", "parent_path", "name", "create_project_folder", "create_project_entry", "directory", "rename_project_entry", "new_name", "delete_project_entry", "sync_file_tree_cache_after_rename", "old_path", "new_path", "remove_file_tree_cache_for_path", "sync_tabs_after_rename", "remove_tabs_for_deleted_entry", "deleted_path", "toggle_read_only", "undo", "redo", "current_line_comment_action", "()Ljava/lang/Boolean;", "current_line_comment_state", "Lcom/jmwl/gostudio/editor/core/editor_line_comment_state;", "toggle_line_comment", "insert_symbol", "symbol", "update_search", "query", "match_case", "whole_word", "regex", "goto_search_result", "forward", "replace_current_match", "replacement", "replace_all_matches", "clear_search", "prepare_tab_editor_for_display", "(Lcom/jmwl/gostudio/editor/session/editor_open_tab;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "prepare_tab_editor", "set_editor_document", "Lio/github/rosemoe/sora/text/Content;", "set_editor_content", "schedule_block_end_hints_update", "apply_textmate_language", "current_textmate_language", "Lio/github/rosemoe/sora/langs/textmate/TextMateLanguage;", "create_configured_textmate_language", "clear_editor_diagnostics", "update_history_state", "cmake_android_config", "Companion", "app_debug"})
public final class editor_activity extends androidx.activity.ComponentActivity {
    private java.io.File project_dir;
    private io.github.rosemoe.sora.widget.CodeEditor editor;
    @org.jetbrains.annotations.NotNull()
    private final com.jmwl.gostudio.editor.session.editor_activity_state state = null;
    @org.jetbrains.annotations.NotNull()
    private final com.jmwl.gostudio.ui.screens.editor.editor_output_panel_state output_panel_state = null;
    private com.jmwl.gostudio.project.detected_project detected_project_info;
    private boolean applying_editor_content = false;
    @org.jetbrains.annotations.Nullable()
    private java.lang.String current_textmate_scope;
    @org.jetbrains.annotations.Nullable()
    private kotlinx.coroutines.Job block_hint_job;
    @org.jetbrains.annotations.Nullable()
    private kotlinx.coroutines.Job cmake_configure_job;
    @org.jetbrains.annotations.Nullable()
    private kotlinx.coroutines.Job cmake_build_job;
    @org.jetbrains.annotations.Nullable()
    private kotlinx.coroutines.Job file_tree_job;
    private boolean textmate_prewarm_started = false;
    @org.jetbrains.annotations.Nullable()
    private com.jmwl.gostudio.lsp.clangd.clangd_lsp_project clangd_project;
    @org.jetbrains.annotations.Nullable()
    private kotlinx.coroutines.Job clangd_connect_job;
    @org.jetbrains.annotations.NotNull()
    private final java.util.Set<java.lang.String> clangd_skipped_files = null;
    @org.jetbrains.annotations.NotNull()
    private final java.util.Map<java.lang.String, java.util.List<com.jmwl.gostudio.editor.model.editor_file_node>> file_tree_children_cache = null;
    private com.jmwl.gostudio.editor.core.editor_search_controller search_controller;
    private com.jmwl.gostudio.activity.editor_tab_lifecycle tab_lifecycle;
    @org.jetbrains.annotations.NotNull()
    private final androidx.activity.result.ActivityResultLauncher<java.lang.String> import_editor_font_launcher = null;
    private static final long block_end_hint_update_delay_ms = 180L;
    private static final long initial_editor_styles_timeout_ms = 800L;
    @org.jetbrains.annotations.NotNull()
    private static final com.jmwl.gostudio.activity.editor_activity.Companion Companion = null;
    
    public editor_activity() {
        super();
    }
    
    @java.lang.Override()
    protected void onCreate(@org.jetbrains.annotations.Nullable()
    android.os.Bundle savedInstanceState) {
    }
    
    @java.lang.Override()
    protected void onDestroy() {
    }
    
    private final void append_detected_project_log() {
    }
    
    @androidx.compose.runtime.Composable()
    private final void editor_activity_content() {
    }
    
    private final com.jmwl.gostudio.activity.editor_tab_lifecycle create_tab_lifecycle() {
        return null;
    }
    
    private final io.github.rosemoe.sora.widget.CodeEditor create_code_editor() {
        return null;
    }
    
    private final io.github.rosemoe.sora.widget.CodeEditor create_tab_editor(java.lang.String file_path) {
        return null;
    }
    
    private final void with_applying_editor_content(kotlin.jvm.functions.Function0<kotlin.Unit> action) {
    }
    
    private final void handle_editor_content_changed() {
    }
    
    private final void handle_editor_selection_changed(io.github.rosemoe.sora.widget.CodeEditor changed_editor) {
    }
    
    private final void update_editor_settings(com.jmwl.gostudio.editor.model.editor_settings_state settings) {
    }
    
    private final void apply_editor_settings(com.jmwl.gostudio.editor.model.editor_settings_state settings) {
    }
    
    private final java.util.List<io.github.rosemoe.sora.widget.CodeEditor> open_editors() {
        return null;
    }
    
    private final void apply_colors_to_open_editors() {
    }
    
    private final void apply_current_editor_behavior_settings(io.github.rosemoe.sora.widget.CodeEditor target, com.jmwl.gostudio.editor.model.editor_settings_state settings) {
    }
    
    private final void request_import_editor_font() {
    }
    
    private final void apply_project_config(com.jmwl.gostudio.project.project_ide_config config, kotlin.jvm.functions.Function0<kotlin.Unit> on_saved) {
    }
    
    private final void initialize_project() {
    }
    
    private final void prewarm_textmate_languages() {
    }
    
    private final void import_editor_font_from_uri(android.net.Uri uri) {
    }
    
    private final void request_open_file(java.lang.String file_path) {
    }
    
    private final void request_select_tab(java.lang.String file_path) {
    }
    
    private final void request_close_tab(java.lang.String file_path) {
    }
    
    private final void request_close_other_tabs(java.lang.String keep_file_path) {
    }
    
    private final void request_close_all_tabs() {
    }
    
    private final void request_close_editor() {
    }
    
    private final void confirm_close_editor() {
    }
    
    private final void close_editor_after_confirmation() {
    }
    
    private final void request_save_file(boolean show_toast, kotlin.jvm.functions.Function0<kotlin.Unit> on_saved) {
    }
    
    private final void format_current_file() {
    }
    
    private final void handle_build_button_click() {
    }
    
    private final void build_cmake_project() {
    }
    
    private final void configure_cmake_project() {
    }
    
    private final void configure_cmake_project_if_needed() {
    }
    
    private final void configure_cmake_project(boolean clean_build, boolean show_toast, kotlin.jvm.functions.Function0<kotlin.Unit> on_success) {
    }
    
    private final java.lang.String create_cmake_configure_command(java.lang.String source_dir, java.lang.String build_dir, java.lang.String cmake_toolchain_file, java.lang.String existing_generator, com.jmwl.gostudio.activity.editor_activity.cmake_android_config android_config) {
        return null;
    }
    
    private final com.jmwl.gostudio.activity.editor_activity.cmake_android_config project_cmake_config(java.io.File project_dir) {
        return null;
    }
    
    private final com.jmwl.gostudio.activity.editor_activity.cmake_android_config infer_cmake_android_config(java.io.File project_dir) {
        return null;
    }
    
    private final java.lang.String infer_cmake_android_abi(java.lang.String content) {
        return null;
    }
    
    private final java.lang.String infer_cmake_android_platform(java.lang.String content) {
        return null;
    }
    
    private final boolean uses_android_vulkan(java.lang.String content) {
        return false;
    }
    
    private final java.lang.String enabled_cmake_args(java.lang.String value) {
        return null;
    }
    
    private final java.lang.String read_existing_cmake_generator(java.lang.String build_dir) {
        return null;
    }
    
    private final boolean reset_cmake_build_dir(java.lang.String build_dir) {
        return false;
    }
    
    private final com.jmwl.gostudio.ui.screens.editor.editor_output_line_level output_level_for_cmake_line(java.lang.String line) {
        return null;
    }
    
    private final java.lang.String shell_quote(java.lang.String value) {
        return null;
    }
    
    private final void save_pending_action() {
    }
    
    private final void discard_pending_action() {
    }
    
    private final void run_pending_action() {
    }
    
    private final void open_file(java.lang.String file_path) {
    }
    
    private final void open_file_at(java.lang.String file_path, int line, int column) {
    }
    
    private final com.jmwl.gostudio.editor.session.editor_open_tab create_open_tab(com.jmwl.gostudio.editor.core.editor_loaded_file loaded_file) {
        return null;
    }
    
    private final java.lang.Object open_loaded_file_tab(com.jmwl.gostudio.editor.core.editor_loaded_file loaded_file, kotlin.coroutines.Continuation<? super com.jmwl.gostudio.editor.session.editor_open_tab> $completion) {
        return null;
    }
    
    private final void move_cursor_to(int line, int column) {
    }
    
    private final java.lang.Object restore_pinned_tabs(kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
    
    private final void save_pinned_tabs() {
    }
    
    private final java.lang.Object save_dirty_open_files(boolean show_toast, kotlin.coroutines.Continuation<? super java.lang.Boolean> $completion) {
        return null;
    }
    
    private final java.lang.Object save_current_file(boolean show_toast, kotlin.coroutines.Continuation<? super java.lang.Boolean> $completion) {
        return null;
    }
    
    private final void configure_cmake_after_cmakelists_save(java.lang.String file_path) {
    }
    
    private final void toggle_pin_tab(java.lang.String file_path) {
    }
    
    private final void close_tab(java.lang.String file_path) {
    }
    
    private final void close_other_tabs(java.lang.String keep_file_path) {
    }
    
    private final void close_all_tabs() {
    }
    
    private final void reorder_tabs_keep_active() {
    }
    
    private final void attach_editor_tab(int index, boolean capture_current) {
    }
    
    private final void activate_editor_tab(int index, com.jmwl.gostudio.editor.session.editor_open_tab tab) {
    }
    
    private final java.util.Set<io.github.rosemoe.sora.lsp.client.languageserver.LspFeature> disabled_clangd_features(com.jmwl.gostudio.editor.model.editor_settings_state settings) {
        return null;
    }
    
    private final void reset_clangd_project() {
    }
    
    private final void restore_editor_languages_from_clangd() {
    }
    
    private final void connect_clangd_if_needed(com.jmwl.gostudio.editor.session.editor_open_tab tab) {
    }
    
    private final java.lang.String clean_clangd_log_line(java.lang.String line) {
        return null;
    }
    
    private final java.lang.String configured_ndk_clang_prefix() {
        return null;
    }
    
    private final void log_clangd_skip_once(java.io.File file, java.lang.String reason) {
    }
    
    private final void log_clangd_status(java.io.File file, io.github.rosemoe.sora.lsp.editor.LspEditorStatus status) {
    }
    
    private final void restore_editor_selection(com.jmwl.gostudio.editor.session.editor_open_tab tab) {
    }
    
    private final void reset_editor_state() {
    }
    
    private final void capture_active_tab_state() {
    }
    
    private final com.jmwl.gostudio.editor.session.editor_open_tab active_tab() {
        return null;
    }
    
    private final void release_tab_editor(com.jmwl.gostudio.editor.session.editor_open_tab tab) {
    }
    
    private final int find_tab_index(java.lang.String file_path) {
        return 0;
    }
    
    private final void reload_file_tree(kotlin.jvm.functions.Function0<kotlin.Unit> on_complete) {
    }
    
    private final void toggle_directory(java.lang.String path) {
    }
    
    private final void refresh_file_tree(java.lang.String directory_path, kotlin.jvm.functions.Function0<kotlin.Unit> on_complete) {
    }
    
    private final void load_file_tree_directory_if_needed(java.lang.String path, boolean force, kotlin.jvm.functions.Function0<kotlin.Unit> on_complete) {
    }
    
    private final void create_project_file(java.lang.String parent_path, java.lang.String name) {
    }
    
    private final void create_project_folder(java.lang.String parent_path, java.lang.String name) {
    }
    
    private final void create_project_entry(java.lang.String parent_path, java.lang.String name, boolean directory) {
    }
    
    private final void rename_project_entry(java.lang.String path, java.lang.String new_name) {
    }
    
    private final void delete_project_entry(java.lang.String path) {
    }
    
    private final void sync_file_tree_cache_after_rename(java.lang.String old_path, java.lang.String new_path) {
    }
    
    private final void remove_file_tree_cache_for_path(java.lang.String path) {
    }
    
    private final void sync_tabs_after_rename(java.lang.String old_path, java.lang.String new_path) {
    }
    
    private final void remove_tabs_for_deleted_entry(java.lang.String deleted_path) {
    }
    
    private final void toggle_read_only() {
    }
    
    private final void undo() {
    }
    
    private final void redo() {
    }
    
    private final java.lang.Boolean current_line_comment_action() {
        return null;
    }
    
    private final com.jmwl.gostudio.editor.core.editor_line_comment_state current_line_comment_state() {
        return null;
    }
    
    private final void toggle_line_comment() {
    }
    
    private final void insert_symbol(java.lang.String symbol) {
    }
    
    private final boolean update_search(java.lang.String query, boolean match_case, boolean whole_word, boolean regex) {
        return false;
    }
    
    private final void goto_search_result(boolean forward) {
    }
    
    private final void replace_current_match(java.lang.String replacement) {
    }
    
    private final void replace_all_matches(java.lang.String replacement) {
    }
    
    private final void clear_search() {
    }
    
    private final java.lang.Object prepare_tab_editor_for_display(com.jmwl.gostudio.editor.session.editor_open_tab tab, kotlin.coroutines.Continuation<? super io.github.rosemoe.sora.widget.CodeEditor> $completion) {
        return null;
    }
    
    private final io.github.rosemoe.sora.widget.CodeEditor prepare_tab_editor(com.jmwl.gostudio.editor.session.editor_open_tab tab) {
        return null;
    }
    
    private final void set_editor_document(io.github.rosemoe.sora.text.Content content) {
    }
    
    private final void set_editor_content(java.lang.String content) {
    }
    
    private final void schedule_block_end_hints_update() {
    }
    
    private final void apply_textmate_language(java.lang.String file_path) {
    }
    
    private final io.github.rosemoe.sora.langs.textmate.TextMateLanguage current_textmate_language(io.github.rosemoe.sora.widget.CodeEditor target) {
        return null;
    }
    
    private final io.github.rosemoe.sora.langs.textmate.TextMateLanguage create_configured_textmate_language(java.lang.String file_path) {
        return null;
    }
    
    private final void clear_editor_diagnostics() {
    }
    
    private final void update_history_state() {
    }
    
    @kotlin.Metadata(mv = {2, 3, 0}, k = 1, xi = 48, d1 = {"\u0000\u0014\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0003\n\u0002\u0010\t\n\u0002\b\u0002\b\u0082\u0003\u0018\u00002\u00020\u0001B\t\b\u0002\u00a2\u0006\u0004\b\u0002\u0010\u0003R\u000e\u0010\u0004\u001a\u00020\u0005X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0006\u001a\u00020\u0005X\u0082T\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0007"}, d2 = {"Lcom/jmwl/gostudio/activity/editor_activity$Companion;", "", "<init>", "()V", "block_end_hint_update_delay_ms", "", "initial_editor_styles_timeout_ms", "app_debug"})
    static final class Companion {
        
        private Companion() {
            super();
        }
    }
    
    @kotlin.Metadata(mv = {2, 3, 0}, k = 1, xi = 48, d1 = {"\u0000\"\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0004\n\u0002\u0010\b\n\u0002\b\u0013\n\u0002\u0010\u000b\n\u0002\b\u0004\b\u0082\b\u0018\u00002\u00020\u0001BK\u0012\n\b\u0002\u0010\u0002\u001a\u0004\u0018\u00010\u0003\u0012\n\b\u0002\u0010\u0004\u001a\u0004\u0018\u00010\u0003\u0012\n\b\u0002\u0010\u0005\u001a\u0004\u0018\u00010\u0003\u0012\n\b\u0002\u0010\u0006\u001a\u0004\u0018\u00010\u0003\u0012\b\b\u0002\u0010\u0007\u001a\u00020\b\u0012\b\b\u0002\u0010\t\u001a\u00020\u0003\u00a2\u0006\u0004\b\n\u0010\u000bJ\u000b\u0010\u0014\u001a\u0004\u0018\u00010\u0003H\u00c6\u0003J\u000b\u0010\u0015\u001a\u0004\u0018\u00010\u0003H\u00c6\u0003J\u000b\u0010\u0016\u001a\u0004\u0018\u00010\u0003H\u00c6\u0003J\u000b\u0010\u0017\u001a\u0004\u0018\u00010\u0003H\u00c6\u0003J\t\u0010\u0018\u001a\u00020\bH\u00c6\u0003J\t\u0010\u0019\u001a\u00020\u0003H\u00c6\u0003JM\u0010\u001a\u001a\u00020\u00002\n\b\u0002\u0010\u0002\u001a\u0004\u0018\u00010\u00032\n\b\u0002\u0010\u0004\u001a\u0004\u0018\u00010\u00032\n\b\u0002\u0010\u0005\u001a\u0004\u0018\u00010\u00032\n\b\u0002\u0010\u0006\u001a\u0004\u0018\u00010\u00032\b\b\u0002\u0010\u0007\u001a\u00020\b2\b\b\u0002\u0010\t\u001a\u00020\u0003H\u00c6\u0001J\u0014\u0010\u001b\u001a\u00020\u001c2\b\u0010\u001d\u001a\u0004\u0018\u00010\u0001H\u00d6\u0083\u0004J\n\u0010\u001e\u001a\u00020\bH\u00d6\u0081\u0004J\n\u0010\u001f\u001a\u00020\u0003H\u00d6\u0081\u0004R\u0013\u0010\u0002\u001a\u0004\u0018\u00010\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\f\u0010\rR\u0013\u0010\u0004\u001a\u0004\u0018\u00010\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u000e\u0010\rR\u0013\u0010\u0005\u001a\u0004\u0018\u00010\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u000f\u0010\rR\u0013\u0010\u0006\u001a\u0004\u0018\u00010\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0010\u0010\rR\u0011\u0010\u0007\u001a\u00020\b\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0011\u0010\u0012R\u0011\u0010\t\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0013\u0010\r\u00a8\u0006 "}, d2 = {"Lcom/jmwl/gostudio/activity/editor_activity$cmake_android_config;", "", "abi", "", "platform", "cpp_standard", "build_type", "parallel_jobs", "", "extra_cmake_args", "<init>", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;ILjava/lang/String;)V", "getAbi", "()Ljava/lang/String;", "getPlatform", "getCpp_standard", "getBuild_type", "getParallel_jobs", "()I", "getExtra_cmake_args", "component1", "component2", "component3", "component4", "component5", "component6", "copy", "equals", "", "other", "hashCode", "toString", "app_debug"})
    static final class cmake_android_config {
        @org.jetbrains.annotations.Nullable()
        private final java.lang.String abi = null;
        @org.jetbrains.annotations.Nullable()
        private final java.lang.String platform = null;
        @org.jetbrains.annotations.Nullable()
        private final java.lang.String cpp_standard = null;
        @org.jetbrains.annotations.Nullable()
        private final java.lang.String build_type = null;
        private final int parallel_jobs = 0;
        @org.jetbrains.annotations.NotNull()
        private final java.lang.String extra_cmake_args = null;
        
        public cmake_android_config(@org.jetbrains.annotations.Nullable()
        java.lang.String abi, @org.jetbrains.annotations.Nullable()
        java.lang.String platform, @org.jetbrains.annotations.Nullable()
        java.lang.String cpp_standard, @org.jetbrains.annotations.Nullable()
        java.lang.String build_type, int parallel_jobs, @org.jetbrains.annotations.NotNull()
        java.lang.String extra_cmake_args) {
            super();
        }
        
        @org.jetbrains.annotations.Nullable()
        public final java.lang.String getAbi() {
            return null;
        }
        
        @org.jetbrains.annotations.Nullable()
        public final java.lang.String getPlatform() {
            return null;
        }
        
        @org.jetbrains.annotations.Nullable()
        public final java.lang.String getCpp_standard() {
            return null;
        }
        
        @org.jetbrains.annotations.Nullable()
        public final java.lang.String getBuild_type() {
            return null;
        }
        
        public final int getParallel_jobs() {
            return 0;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.lang.String getExtra_cmake_args() {
            return null;
        }
        
        public cmake_android_config() {
            super();
        }
        
        @org.jetbrains.annotations.Nullable()
        public final java.lang.String component1() {
            return null;
        }
        
        @org.jetbrains.annotations.Nullable()
        public final java.lang.String component2() {
            return null;
        }
        
        @org.jetbrains.annotations.Nullable()
        public final java.lang.String component3() {
            return null;
        }
        
        @org.jetbrains.annotations.Nullable()
        public final java.lang.String component4() {
            return null;
        }
        
        public final int component5() {
            return 0;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.lang.String component6() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final com.jmwl.gostudio.activity.editor_activity.cmake_android_config copy(@org.jetbrains.annotations.Nullable()
        java.lang.String abi, @org.jetbrains.annotations.Nullable()
        java.lang.String platform, @org.jetbrains.annotations.Nullable()
        java.lang.String cpp_standard, @org.jetbrains.annotations.Nullable()
        java.lang.String build_type, int parallel_jobs, @org.jetbrains.annotations.NotNull()
        java.lang.String extra_cmake_args) {
            return null;
        }
        
        @java.lang.Override()
        public boolean equals(@org.jetbrains.annotations.Nullable()
        java.lang.Object other) {
            return false;
        }
        
        @java.lang.Override()
        public int hashCode() {
            return 0;
        }
        
        @java.lang.Override()
        @org.jetbrains.annotations.NotNull()
        public java.lang.String toString() {
            return null;
        }
    }
}