package org.neonalig.createpop.client;

import net.minecraft.client.Minecraft;
import net.minecraft.util.RandomSource;
import org.neonalig.createpop.network.OpenBrewersNotebookPayload;
import org.neonalig.createpop.network.OpenSodaNamePromptPayload;
import org.neonalig.createpop.soda.SodaNameGenerator;

public final class ModPayloadsClient {
    private ModPayloadsClient() {
    }

    public static void openPrompt(OpenSodaNamePromptPayload payload) {
        Minecraft.getInstance().setScreen(new SodaNamePromptScreen(
                payload.sodaKey(),
                payload.suggestedName(),
                () -> SodaNameGenerator.randomName(RandomSource.create())
        ));
    }

    public static void openNotebook(OpenBrewersNotebookPayload payload) {
        Minecraft.getInstance().setScreen(new BrewersNotebookScreen(payload.notebookData(), payload.mainHand()));
    }

    public static void openGuide() {
        Minecraft.getInstance().setScreen(new BrewersGuideScreen());
    }
}


