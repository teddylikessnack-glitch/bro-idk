package dev.bearclient;

public enum Corner {
    TOP_LEFT("TL", false, false),
    TOP_RIGHT("TR", true, false),
    BOTTOM_LEFT("BL", false, true),
    BOTTOM_RIGHT("BR", true, true);

    public static final Corner[] ALL = values();

    public final String tag;
    public final boolean right, bottom;

    Corner(String tag, boolean right, boolean bottom) {
        this.tag = tag;
        this.right = right;
        this.bottom = bottom;
    }

    public Corner next() { return ALL[(ordinal() + 1) % ALL.length]; }

    public int x(int screenW, int boxW, int margin) { return right ? screenW - margin - boxW : margin; }

    public int y(int screenH, int boxH, int margin) { return bottom ? screenH - margin - boxH : margin; }
}
