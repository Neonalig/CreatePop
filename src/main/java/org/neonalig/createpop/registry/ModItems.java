package org.neonalig.createpop.registry;

import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import org.neonalig.createpop.CreatePop;
import org.neonalig.createpop.item.BrewersGuideItem;
import org.neonalig.createpop.item.BrewersNotebookItem;

public final class ModItems {
    public static final DeferredItem<BrewersGuideItem> BREWERS_GUIDE = CreatePop.ITEMS.register(
            "brewers_guide",
            () -> new BrewersGuideItem(new Item.Properties().stacksTo(1))
    );

    public static final DeferredItem<BrewersNotebookItem> BREWERS_NOTEBOOK = CreatePop.ITEMS.register(
            "brewers_notebook",
            () -> new BrewersNotebookItem(new Item.Properties().stacksTo(1))
    );

    private ModItems() {
    }

    public static void init() {
        // Forces class initialization during mod construction so item registrations
        // are created before DeferredRegister closes.
    }
}


