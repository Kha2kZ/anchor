package com.khai2kzgaming.anchormacro.screen;

import com.khai2kzgaming.anchormacro.AnchorMacroClient;
import com.khai2kzgaming.anchormacro.config.AnchorMacroConfig;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public final class AnchorMacroScreen extends Screen {
    private final Screen parent;

    public AnchorMacroScreen(Screen parent) {
        super(Text.translatable("screen.anchor-macro.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int buttonWidth = 220;
        int buttonX = centerX - buttonWidth / 2;

        addDrawableChild(ButtonWidget.builder(
                modToggleLabel(),
                button -> toggleMod()
        ).dimensions(buttonX, 45, buttonWidth, 20).build());

        addDrawableChild(ButtonWidget.builder(
                modeLabel(AnchorMacroConfig.Mode.AUTO_GLOWSTONE),
                button -> setMode(AnchorMacroConfig.Mode.AUTO_GLOWSTONE)
        ).dimensions(buttonX, 70, buttonWidth, 20).build());

        addDrawableChild(ButtonWidget.builder(
                modeLabel(AnchorMacroConfig.Mode.SAFE_ANCHOR),
                button -> setMode(AnchorMacroConfig.Mode.SAFE_ANCHOR)
        ).dimensions(buttonX, 95, buttonWidth, 20).build());

        addDrawableChild(ButtonWidget.builder(
                modeLabel(AnchorMacroConfig.Mode.FULL_SAFE_ANCHOR),
                button -> setMode(AnchorMacroConfig.Mode.FULL_SAFE_ANCHOR)
        ).dimensions(buttonX, 120, buttonWidth, 20).build());

        addDrawableChild(ButtonWidget.builder(
                modeLabel(AnchorMacroConfig.Mode.FULL_ANCHOR),
                button -> setMode(AnchorMacroConfig.Mode.FULL_ANCHOR)
        ).dimensions(buttonX, 145, buttonWidth, 20).build());

        int slotButtonWidth = 24;
        int slotsStartX = centerX - (slotButtonWidth * 9 + 4 * 8) / 2;
        for (int slot = 0; slot < 9; slot++) {
            int x = slotsStartX + slot * (slotButtonWidth + 4);
            final int selectedSlot = slot;
            addDrawableChild(ButtonWidget.builder(
                    Text.literal(Integer.toString(slot + 1)),
                    button -> setSafeHotbarSlot(selectedSlot)
            ).dimensions(x, 205, slotButtonWidth, 20).build());
        }

        addDrawableChild(ButtonWidget.builder(
                Text.translatable("gui.done"),
                button -> close()
        ).dimensions(buttonX, 245, buttonWidth, 20).build());
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
        context.drawCenteredTextWithShadow(
                this.textRenderer,
                Text.translatable("screen.anchor-macro.safe_slot"),
                this.width / 2,
                185,
                0xFFFFFF
        );
        context.drawCenteredTextWithShadow(
                this.textRenderer,
                Text.translatable("screen.anchor-macro.selected_slot",
                        AnchorMacroClient.CONFIG.safeHotbarSlot + 1),
                this.width / 2,
                235,
                0xAAAAAA
        );
        super.render(context, mouseX, mouseY, delta);
    }

    private Text modeLabel(AnchorMacroConfig.Mode mode) {
        boolean selected = AnchorMacroClient.CONFIG.enabled
                && AnchorMacroClient.CONFIG.mode == mode;
        String key = switch (mode) {
            case AUTO_GLOWSTONE -> "screen.anchor-macro.auto_glowstone";
            case SAFE_ANCHOR -> "screen.anchor-macro.safe_anchor";
            case FULL_SAFE_ANCHOR -> "screen.anchor-macro.full_safe_anchor";
            case FULL_ANCHOR -> "screen.anchor-macro.full_anchor";
        };
        return Text.translatable(key, selected ? "ON" : "OFF");
    }

    private Text modToggleLabel() {
        return Text.translatable(
                "screen.anchor-macro.mod_toggle",
                AnchorMacroClient.CONFIG.enabled ? "ON" : "OFF"
        );
    }

    private void toggleMod() {
        AnchorMacroClient.CONFIG.enabled = !AnchorMacroClient.CONFIG.enabled;
        AnchorMacroClient.CONFIG.save();
        clearAndInit();
    }

    private void setMode(AnchorMacroConfig.Mode mode) {
        if (AnchorMacroClient.CONFIG.mode == mode) {
            AnchorMacroClient.CONFIG.enabled = !AnchorMacroClient.CONFIG.enabled;
        } else {
            AnchorMacroClient.CONFIG.mode = mode;
            AnchorMacroClient.CONFIG.enabled = true;
        }
        AnchorMacroClient.CONFIG.save();
        clearAndInit();
    }

    private void setSafeHotbarSlot(int slot) {
        AnchorMacroClient.CONFIG.safeHotbarSlot = AnchorMacroConfig.clampHotbarSlot(slot);
        AnchorMacroClient.CONFIG.save();
        clearAndInit();
    }
}