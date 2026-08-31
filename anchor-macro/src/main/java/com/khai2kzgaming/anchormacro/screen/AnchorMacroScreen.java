package com.khai2kzgaming.anchormacro.screen;

import com.khai2kzgaming.anchormacro.AnchorMacroClient;
import com.khai2kzgaming.anchormacro.config.AnchorMacroConfig;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.option.ControlsOptionsScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

public final class AnchorMacroScreen extends Screen {
    private final Screen parent;
    private Tab activeTab = Tab.GENERIC;
    private AnchorMacroConfig.Mode capturingModifier;

    public AnchorMacroScreen(Screen parent) {
        super(Text.translatable("screen.anchor-macro.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int tabWidth = 108;

        addDrawableChild(ButtonWidget.builder(
                Text.translatable("screen.anchor-macro.generic_tab"),
                button -> switchTab(Tab.GENERIC)
        ).dimensions(centerX - tabWidth - 2, 40, tabWidth, 20).build());

        addDrawableChild(ButtonWidget.builder(
                Text.translatable("screen.anchor-macro.keybinds_tab"),
                button -> switchTab(Tab.KEYBINDS)
        ).dimensions(centerX + 2, 40, tabWidth, 20).build());

        if (activeTab == Tab.GENERIC) {
            initGenericTab(centerX);
        } else {
            initKeybindsTab(centerX);
        }
    }

    private void initGenericTab(int centerX) {
        int buttonWidth = 220;
        int buttonX = centerX - buttonWidth / 2;

        addDrawableChild(ButtonWidget.builder(
                modToggleLabel(),
                button -> toggleMod()
        ).dimensions(buttonX, 70, buttonWidth, 20).build());

        AnchorMacroConfig.Mode[] modes = AnchorMacroConfig.Mode.values();
        for (int index = 0; index < modes.length; index++) {
            AnchorMacroConfig.Mode mode = modes[index];
            addDrawableChild(ButtonWidget.builder(
                    modeLabel(mode),
                    button -> setMode(mode)
            ).dimensions(buttonX, 95 + index * 25, buttonWidth, 20).build());
        }

        int slotButtonWidth = 24;
        int slotsStartX = centerX - (slotButtonWidth * 9 + 4 * 8) / 2;
        for (int slot = 0; slot < 9; slot++) {
            int x = slotsStartX + slot * (slotButtonWidth + 4);
            final int selectedSlot = slot;
            addDrawableChild(ButtonWidget.builder(
                    Text.literal(Integer.toString(slot + 1)),
                    button -> setSafeHotbarSlot(selectedSlot)
            ).dimensions(x, 225, slotButtonWidth, 20).build());
        }

        addDrawableChild(ButtonWidget.builder(
                Text.translatable("gui.done"),
                button -> close()
        ).dimensions(buttonX, 275, buttonWidth, 20).build());
    }

    private void initKeybindsTab(int centerX) {
        int buttonWidth = 220;
        int buttonX = centerX - buttonWidth / 2;

        addDrawableChild(ButtonWidget.builder(
                Text.translatable("screen.anchor-macro.open_controls"),
                button -> client.setScreen(new ControlsOptionsScreen(this, client.options))
        ).dimensions(buttonX, 70, buttonWidth, 20).build());

        AnchorMacroConfig.Mode[] modes = AnchorMacroConfig.Mode.values();
        for (int index = 0; index < modes.length; index++) {
            AnchorMacroConfig.Mode mode = modes[index];
            int y = 105 + index * 40;
            addDrawableChild(ButtonWidget.builder(
                    modifierButtonLabel(mode),
                    button -> beginModifierCapture(mode)
            ).dimensions(buttonX, y, buttonWidth, 20).build());
            addDrawableChild(ButtonWidget.builder(
                    Text.translatable("screen.anchor-macro.clear_modifier"),
                    button -> clearModifier(mode)
            ).dimensions(buttonX + buttonWidth + 8, y, 70, 20).build());
        }

        addDrawableChild(ButtonWidget.builder(
                Text.translatable("gui.done"),
                button -> close()
        ).dimensions(buttonX, 280, buttonWidth, 20).build());
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (capturingModifier != null) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_BACKSPACE) {
                clearModifier(capturingModifier);
            } else if (keyCode != GLFW.GLFW_KEY_UNKNOWN) {
                AnchorMacroClient.CONFIG.setModifier(capturingModifier, keyCode);
                AnchorMacroClient.CONFIG.save();
                capturingModifier = null;
                clearAndInit();
            }
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void close() {
        this.client.setScreen(parent);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(
                this.textRenderer,
                this.title,
                this.width / 2,
                20,
                0xFFFFFF
        );

        if (activeTab == Tab.GENERIC) {
            context.drawCenteredTextWithShadow(
                    this.textRenderer,
                    Text.translatable("screen.anchor-macro.safe_slot"),
                    this.width / 2,
                    210,
                    0xFFFFFF
            );
            context.drawCenteredTextWithShadow(
                    this.textRenderer,
                    Text.translatable("screen.anchor-macro.selected_slot",
                            AnchorMacroClient.CONFIG.safeHotbarSlot + 1),
                    this.width / 2,
                    260,
                    0xAAAAAA
            );
        } else {
            context.drawCenteredTextWithShadow(
                    this.textRenderer,
                    Text.translatable("screen.anchor-macro.keybind_help"),
                    this.width / 2,
                    95,
                    0xAAAAAA
            );
            if (capturingModifier != null) {
                context.drawCenteredTextWithShadow(
                        this.textRenderer,
                        Text.translatable("screen.anchor-macro.press_modifier"),
                        this.width / 2,
                        270,
                        0xFFCC55
                );
            }
        }

        super.render(context, mouseX, mouseY, delta);
    }

    private Text modeLabel(AnchorMacroConfig.Mode mode) {
        boolean selected = AnchorMacroClient.CONFIG.enabled
                && AnchorMacroClient.CONFIG.mode == mode;
        return Text.translatable(modeTranslationKey(mode), selected ? "ON" : "OFF");
    }

    private Text modToggleLabel() {
        return Text.translatable(
                "screen.anchor-macro.mod_toggle",
                AnchorMacroClient.CONFIG.enabled ? "ON" : "OFF"
        );
    }

    private Text modifierButtonLabel(AnchorMacroConfig.Mode mode) {
        if (capturingModifier == mode) {
            return Text.translatable("screen.anchor-macro.press_modifier");
        }
        return Text.translatable(
                "screen.anchor-macro.modifier",
                modeShortName(mode),
                modifierLabel(AnchorMacroClient.CONFIG.getModifier(mode)),
                AnchorMacroClient.getModeToggleKey(mode).getBoundKeyLocalizedText()
        );
    }

    private Text modifierLabel(int keyCode) {
        if (keyCode == GLFW.GLFW_KEY_UNKNOWN) {
            return Text.translatable("screen.anchor-macro.none");
        }
        return InputUtil.fromKeyCode(keyCode, 0).getLocalizedText();
    }

    private String modeShortName(AnchorMacroConfig.Mode mode) {
        return switch (mode) {
            case AUTO_GLOWSTONE -> "Auto Glowstone";
            case SAFE_ANCHOR -> "Safe Anchor";
            case FULL_SAFE_ANCHOR -> "Full Safe Anchor";
            case FULL_ANCHOR -> "Full Anchor";
        };
    }

    private String modeTranslationKey(AnchorMacroConfig.Mode mode) {
        return switch (mode) {
            case AUTO_GLOWSTONE -> "screen.anchor-macro.auto_glowstone";
            case SAFE_ANCHOR -> "screen.anchor-macro.safe_anchor";
            case FULL_SAFE_ANCHOR -> "screen.anchor-macro.full_safe_anchor";
            case FULL_ANCHOR -> "screen.anchor-macro.full_anchor";
        };
    }

    private void switchTab(Tab tab) {
        activeTab = tab;
        capturingModifier = null;
        clearAndInit();
    }

    private void toggleMod() {
        AnchorMacroClient.CONFIG.enabled = !AnchorMacroClient.CONFIG.enabled;
        AnchorMacroClient.CONFIG.save();
        clearAndInit();
    }

    private void setMode(AnchorMacroConfig.Mode mode) {
        AnchorMacroClient.toggleMode(mode);
        clearAndInit();
    }

    private void setSafeHotbarSlot(int slot) {
        AnchorMacroClient.CONFIG.safeHotbarSlot = AnchorMacroConfig.clampHotbarSlot(slot);
        AnchorMacroClient.CONFIG.save();
        clearAndInit();
    }

    private void beginModifierCapture(AnchorMacroConfig.Mode mode) {
        capturingModifier = mode;
        clearAndInit();
    }

    private void clearModifier(AnchorMacroConfig.Mode mode) {
        AnchorMacroClient.CONFIG.setModifier(mode, GLFW.GLFW_KEY_UNKNOWN);
        AnchorMacroClient.CONFIG.save();
        capturingModifier = null;
        clearAndInit();
    }

    private enum Tab {
        GENERIC,
        KEYBINDS
    }
}