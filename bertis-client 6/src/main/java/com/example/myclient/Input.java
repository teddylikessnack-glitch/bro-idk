package com.example.myclient;

import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

// Mouse polling for places where we draw our own buttons.
public final class Input {
    private static boolean wasDown;

    private Input() {}

    public static boolean leftDown(Minecraft mc) {
        long handle = GLFW.glfwGetCurrentContext();
        if (handle != 0L && GLFW.glfwGetMouseButton(handle, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS) {
            return true;
        }
        return mc.mouseHandler.isLeftPressed();
    }

    // True once per click. Call it exactly once per frame.
    public static boolean leftClicked(Minecraft mc) {
        boolean down = leftDown(mc);
        boolean clicked = down && !wasDown;
        wasDown = down;
        return clicked;
    }
}
