package dev.bearclient;

public enum Category {
    PERFORMANCE("Performance"),
    DEBUG("Debug"),
    CUSTOMIZATION("Customization"),
    OVERLAY("Overlay");

    public final String label;

    Category(String label) { this.label = label; }
}
