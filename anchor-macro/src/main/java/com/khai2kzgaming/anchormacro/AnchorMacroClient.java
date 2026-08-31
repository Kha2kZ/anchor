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

    private static final KeyBinding[] MODE_TOGGLE_KEYS =
            new KeyBinding[AnchorMacroConfig.Mode.values().length];
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

        for (AnchorMacroConfig.Mode mode : AnchorMacroConfig.Mode.values()) {
            MODE_TOGGLE_KEYS[mode.ordinal()] = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                    toggleKeyTranslationKey(mode),
                    InputUtil.Type.KEYSYM,
                    GLFW.GLFW_KEY_UNKNOWN,
                    "category.anchor-macro"
            ));
        }

        ClientTickEvents.END_CLIENT_TICK.register(AnchorMacroClient::onClientTick);
    }

    private static void onClientTick(MinecraftClient client) {
        boolean modeToggleTriggered = processModeToggleKeys(client);

        // When a modifier chord uses O, consume the menu binding for this tick
        // so a chord such as O+F does not also open the menu.
        if (modeToggleTriggered) {
            while (openMenuKey.wasPressed()) {
                // Consume the menu key event.
            }
        } else {
            while (openMenuKey.wasPressed()) {
                if (client.currentScreen == null) {
                    client.setScreen(new AnchorMacroScreen(null));
                }
            }
        }

        AnchorMacroActions.tick(client);
    }

    private static boolean processModeToggleKeys(MinecraftClient client) {
        boolean triggered = false;
        for (AnchorMacroConfig.Mode mode : AnchorMacroConfig.Mode.values()) {
            KeyBinding keyBinding = MODE_TOGGLE_KEYS[mode.ordinal()];
            while (keyBinding.wasPressed()) {
                int modifier = CONFIG.getModifier(mode);
                boolean modifierPressed = modifier == GLFW.GLFW_KEY_UNKNOWN
                        || InputUtil.isKeyPressed(client.getWindow().getHandle(), modifier);
                if (modifierPressed) {
                    toggleMode(mode);
                    triggered = true;
                }
            }
        }
        return triggered;
    }

    public static KeyBinding getModeToggleKey(AnchorMacroConfig.Mode mode) {
        return MODE_TOGGLE_KEYS[mode.ordinal()];
    }

    public static void toggleMode(AnchorMacroConfig.Mode mode) {
        if (CONFIG.mode == mode) {
            CONFIG.enabled = !CONFIG.enabled;
        } else {
            CONFIG.mode = mode;
            CONFIG.enabled = true;
        }
        CONFIG.save();
    }

    private static String toggleKeyTranslationKey(AnchorMacroConfig.Mode mode) {
        return switch (mode) {
            case AUTO_GLOWSTONE -> "key.anchor-macro.toggle_auto_glowstone";
            case SAFE_ANCHOR -> "key.anchor-macro.toggle_safe_anchor";
            case FULL_SAFE_ANCHOR -> "key.anchor-macro.toggle_full_safe_anchor";
            case FULL_ANCHOR -> "key.anchor-macro.toggle_full_anchor";
        };
    }
}