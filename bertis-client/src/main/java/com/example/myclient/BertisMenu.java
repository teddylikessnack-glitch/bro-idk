package com.example.myclient;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class BertisMenu extends Screen {
    public BertisMenu() {
        super(Component.literal("Bertis Client"));
    }

    private static Component label(int i) {
        return Component.literal(Settings.NAMES[i] + ": " + (Settings.ON[i] ? "ON" : "OFF"));
    }

    @Override
    protected void init() {
        int bw = 150, bh = 20, gap = 6;
        int left = this.width / 2 - bw - gap / 2;
        int top = this.height / 2 - 70;

        for (int i = 0; i < Settings.NAMES.length; i++) {
            final int idx = i;
            int bx = left + (i % 2) * (bw + gap);
            int by = top + (i / 2) * (bh + gap);
            Button b = Button.builder(label(idx), btn -> {
                Settings.ON[idx] = !Settings.ON[idx];
                btn.setMessage(label(idx));
            }).bounds(bx, by, bw, bh).build();
            this.addRenderableWidget(b);
        }

        int rows = (Settings.NAMES.length + 1) / 2;
        int doneY = top + rows * (bh + gap) + 8;
        this.addRenderableWidget(Button.builder(Component.literal("Done"), btn -> this.onClose())
                .bounds(this.width / 2 - 50, doneY, 100, 20).build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        super.extractRenderState(g, mouseX, mouseY, delta);
        String title = "Bertis Client";
        g.text(this.font, title, this.width / 2 - this.font.width(title) / 2,
                this.height / 2 - 92, 0xFFFFD84A, true);
        String hint = "Right Ctrl: show/hide HUD  |  Esc: close";
        g.text(this.font, hint, this.width / 2 - this.font.width(hint) / 2,
                this.height / 2 - 80, 0xFFAAAAAA, true);
    }
}
