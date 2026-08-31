package com.khai2kzgaming.anchormacro.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import net.fabricmc.loader.api.FabricLoader;
import org.lwjgl.glfw.GLFW;

public final class AnchorMacroConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance()
            .getConfigDir()
            .resolve("anchor-macro.json");

    public boolean enabled = true;
    public Mode mode = Mode.AUTO_GLOWSTONE;
    public int safeHotbarSlot = 0;
    public int autoGlowstoneModifier = GLFW.GLFW_KEY_UNKNOWN;
    public int safeAnchorModifier = GLFW.GLFW_KEY_UNKNOWN;
    public int fullSafeAnchorModifier = GLFW.GLFW_KEY_UNKNOWN;
    public int fullAnchorModifier = GLFW.GLFW_KEY_UNKNOWN;

    public static AnchorMacroConfig load() {
        if (!Files.exists(CONFIG_PATH)) {
            return new AnchorMacroConfig();
        }

        try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
            AnchorMacroConfig config = GSON.fromJson(reader, AnchorMacroConfig.class);
            if (config == null) {
                return new AnchorMacroConfig();
            }
            config.safeHotbarSlot = clampHotbarSlot(config.safeHotbarSlot);
            if (config.mode == null) {
                config.mode = Mode.AUTO_GLOWSTONE;
            }
            return config;
        } catch (Exception ignored) {
            return new AnchorMacroConfig();
        }
    }

    public void save() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
                GSON.toJson(this, writer);
            }
        } catch (Exception ignored) {
            // A failed config write should not interrupt gameplay.
        }
    }

    public static int clampHotbarSlot(int slot) {
        return Math.max(0, Math.min(8, slot));
    }

    public int getModifier(Mode mode) {
        return switch (mode) {
            case AUTO_GLOWSTONE -> autoGlowstoneModifier;
            case SAFE_ANCHOR -> safeAnchorModifier;
            case FULL_SAFE_ANCHOR -> fullSafeAnchorModifier;
            case FULL_ANCHOR -> fullAnchorModifier;
        };
    }

    public void setModifier(Mode mode, int keyCode) {
        switch (mode) {
            case AUTO_GLOWSTONE -> autoGlowstoneModifier = keyCode;
            case SAFE_ANCHOR -> safeAnchorModifier = keyCode;
            case FULL_SAFE_ANCHOR -> fullSafeAnchorModifier = keyCode;
            case FULL_ANCHOR -> fullAnchorModifier = keyCode;
        }
    }

    public enum Mode {
        AUTO_GLOWSTONE,
        SAFE_ANCHOR,
        FULL_SAFE_ANCHOR,
        FULL_ANCHOR
    }
}