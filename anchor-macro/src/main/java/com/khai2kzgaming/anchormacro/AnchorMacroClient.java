package com.khai2kzgaming.anchormacro;

import com.khai2kzgaming.anchormacro.config.AnchorMacroConfig;
import com.khai2kzgaming.anchormacro.screen.AnchorMacroScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public final class AnchorMacroClient implements ClientModInitializer {
    public static final AnchorMacroConfig CONFIG = AnchorMacroConfig.load();

    private static KeyBinding openMenuKey;

    @Override
    public void onInitializeClient() {
        AnchorMacroActions.initialize();

        openMenuKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.anchor-macro.open_menu",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_O,
                "category.anchor-macro"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(AnchorMacroClient::onClientTick);
    }

    private static void onClientTick(MinecraftClient client) {
        while (openMenuKey.wasPressed()) {
            if (client.currentScreen == null) {
                client.setScreen(new AnchorMacroScreen(null));
            }
        }

        AnchorMacroActions.tick(client);
    }
}