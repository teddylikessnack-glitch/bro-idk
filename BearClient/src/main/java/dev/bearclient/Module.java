package dev.bearclient;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.List;

public abstract class Module {
    public final String name;
    public final String description;
    public final Category category;

    public Corner corner = Corner.TOP_LEFT;
    public boolean usesCorner;
    /** Text shown in the HUD panel. Rebuilt every few ticks (not every frame) to avoid garbage. */
    public List<String> lines = List.of();

    private boolean enabled;

    protected Module(String name, Category category, String description) {
        this.name = name;
        this.category = category;
        this.description = description;
    }

    /** Marks the module as movable between screen corners (right-click in the menu). */
    public Module at(Corner c) {
        this.corner = c;
        this.usesCorner = true;
        return this;
    }

    public boolean isEnabled() { return enabled; }

    public void toggle() { setEnabled(!enabled); }

    public void setEnabled(boolean value) {
        if (value == enabled) return;
        enabled = value;
        Minecraft mc = Minecraft.getInstance();
        if (value) onEnable(mc); else { onDisable(mc); lines = List.of(); }
    }

    public String key() { return name.replace(' ', '_'); }

    public void onEnable(Minecraft mc) {}
    public void onDisable(Minecraft mc) {}
    /** Called every client tick while enabled. */
    public void onTick(Minecraft mc) {}
    /** Called every ~4 ticks while enabled and in a world. */
    public List<String> buildLines(Minecraft mc) { return List.of(); }
    /** Called every frame while enabled and in a world. */
    public void renderOverlay(GuiGraphicsExtractor g, Minecraft mc, int w, int h) {}
}
