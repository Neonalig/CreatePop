package org.neonalig.createpop.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;
import org.neonalig.createpop.network.SubmitSodaNamePayload;

import javax.annotation.Nonnull;
import java.util.function.Supplier;

public class SodaNamePromptScreen extends Screen {
    private final String sodaKey;
    private final String suggestedName;
    private final Supplier<String> randomNameSupplier;
    private EditBox nameField;

    public SodaNamePromptScreen(String sodaKey, String suggestedName, Supplier<String> randomNameSupplier) {
        super(Component.translatable("createpop.soda_name_prompt.title"));
        this.sodaKey = sodaKey;
        this.suggestedName = suggestedName;
        this.randomNameSupplier = randomNameSupplier;
        this.nameField = new EditBox(this.font, 0, 0, 220, 20, Component.translatable("createpop.soda_name_prompt.field"));
        this.nameField.setValue(suggestedName);
        this.nameField.setMaxLength(48);
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        this.nameField = new EditBox(this.font, centerX - 110, centerY - 10, 220, 20, Component.translatable("createpop.soda_name_prompt.field"));
        this.nameField.setMaxLength(48);
        this.nameField.setValue(this.suggestedName.isBlank() ? randomNameSupplier.get() : this.suggestedName);
        this.addRenderableWidget(this.nameField);
        this.setInitialFocus(this.nameField);

        this.addRenderableWidget(Button.builder(Component.translatable("createpop.soda_name_prompt.accept"), button -> {
            PacketDistributor.sendToServer(new SubmitSodaNamePayload(sodaKey, nameField.getValue().trim()));
            this.onClose();
        }).bounds(centerX - 110, centerY + 20, 68, 20).build());

        this.addRenderableWidget(Button.builder(Component.translatable("createpop.soda_name_prompt.random"), button -> nameField.setValue(randomNameSupplier.get()))
                .bounds(centerX - 35, centerY + 20, 68, 20)
                .build());

        this.addRenderableWidget(Button.builder(Component.translatable("createpop.soda_name_prompt.cancel"), button -> this.onClose())
                .bounds(centerX + 40, centerY + 20, 68, 20)
                .build());
    }

    @Override
    public void render(@Nonnull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.fill(0, 0, this.width, this.height, 0x66000000);
        for (net.minecraft.client.gui.components.Renderable renderable : this.renderables) {
            renderable.render(guiGraphics, mouseX, mouseY, partialTick);
        }
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, (this.height / 2) - 35, 0xFFFFFF);
        guiGraphics.drawCenteredString(this.font, Component.translatable("createpop.soda_name_prompt.subtitle"), this.width / 2, (this.height / 2) - 23, 0xA0A0A0);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}

