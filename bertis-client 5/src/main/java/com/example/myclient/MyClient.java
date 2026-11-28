package com.example.myclient;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;

public class MyClient implements ClientModInitializer {
    public static final String MOD_ID = "myclient";

    private static KeyMapping openMenu;
    private static KeyMapping toggleHud;

    @Override
    public void onInitializeClient() {
        Settings.load();
        Branding.init();
        DiscordRpc.init();

        KeyMapping.Category category = KeyMapping.Category.register(
                Identifier.fromNamespaceAndPath(MOD_ID, "main"));

        openMenu = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.myclient.open_menu", InputConstants.Type.KEYSYM,
                InputConstants.KEY_RSHIFT, category));

        toggleHud = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.myclient.toggle_hud", InputConstants.Type.KEYSYM,
                InputConstants.KEY_RCONTROL, category));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openMenu.consumeClick()) client.setScreenAndShow(new BertisMenu());
            while (toggleHud.consumeClick()) Hud.toggleVisible();
            if (Branding.pendingMenu) {
                Branding.pendingMenu = false;
                client.setScreenAndShow(new BertisMenu());
            }
            Performance.tick(client);
            DiscordRpc.tick(client);
            Hud.tick(client);
        });

        // addLast = drawn after every vanilla HUD element, so it can't be skipped by one of them
        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath(MOD_ID, "info_hud"), Hud::render);
    }
}
