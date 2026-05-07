package org.neonalig.createpop.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.protocol.game.ClientboundOpenBookPacket;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.Filterable;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.WrittenBookItem;
import net.minecraft.world.item.component.WrittenBookContent;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import org.neonalig.createpop.component.BrewersNotebookData;
import org.neonalig.createpop.component.SodaData;
import org.neonalig.createpop.soda.BrewingDiscoveryManager;
import org.neonalig.createpop.soda.SodaTextHelper;

import java.util.ArrayList;
import java.util.List;

public class BrewersNotebookItem extends WrittenBookItem {
    public BrewersNotebookItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide && player.isShiftKeyDown()) {
            if (isTargetingBlock(player)) {
                return InteractionResultHolder.pass(stack);
            }

            int added = BrewingDiscoveryManager.writePlayerRecipesToNotebook(player, stack);
            if (added > 0) {
                player.displayClientMessage(Component.translatable("item.createpop.brewers_notebook.saved_added", added), true);
            } else {
                player.displayClientMessage(Component.translatable("item.createpop.brewers_notebook.saved_none"), true);
            }
            return InteractionResultHolder.sidedSuccess(stack, false);
        }

        if (!level.isClientSide) {
            int learned = BrewingDiscoveryManager.learnFromNotebook(player, stack);
            if (learned > 0) {
                player.displayClientMessage(Component.translatable("item.createpop.brewers_notebook.learned", learned), true);
            }
        }

        stack.set(DataComponents.WRITTEN_BOOK_CONTENT, createNotebookContent(stack));

        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            serverPlayer.connection.send(new ClientboundOpenBookPacket(hand));
            serverPlayer.awardStat(Stats.ITEM_USED.get(this));
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return BrewingDiscoveryManager.notebookHasEntries(stack) || super.isFoil(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable("item.createpop.brewers_notebook.entry_count", BrewingDiscoveryManager.notebookEntryCount(stack))
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.createpop.brewers_notebook.tooltip_hint").withStyle(ChatFormatting.DARK_GRAY));
    }

    private static WrittenBookContent createNotebookContent(ItemStack stack) {
        BrewersNotebookData data = BrewingDiscoveryManager.notebookData(stack);
        List<Filterable<Component>> pages = new ArrayList<>();

        pages.add(page(Component.literal("Brewer's Notebook\n\n")
                .append(Component.translatable("item.createpop.brewers_notebook.entry_count", data.size()))
                .append(Component.literal("\n\nShift + Right Click a vessel with mixed soda to add that specific recipe to your notebook."))));

        if (data.isEmpty()) {
            pages.add(page(Component.literal("No recipes recorded yet.\n\n")
                    .append(Component.literal("Discover mixes by crafting soda or shift-right-clicking fluid vessels and containers that contain mixed soda."))));
        } else {
            List<BrewersNotebookData.Entry> entries = data.entries();
            int recipesStartPage = 2;
            int perContentsPage = 6;
            int contentsPages = (entries.size() + perContentsPage - 1) / perContentsPage;
            recipesStartPage += contentsPages;

            // Build contents pages first to determine page numbers
            List<Integer> recipeStartPages = new ArrayList<>();
            int currentPage = recipesStartPage;
            for (BrewersNotebookData.Entry entry : entries) {
                recipeStartPages.add(currentPage);
                int recipePageCount = buildRecipePages(entry).size();
                currentPage += recipePageCount;
            }

            // Now build the contents table with correct page numbers
            for (int i = 0; i < contentsPages; i++) {
                MutableComponent contents = Component.literal("Contents\n\n");
                int start = i * perContentsPage;
                int end = Math.min(entries.size(), start + perContentsPage);
                for (int index = start; index < end; index++) {
                    int recipePage = recipeStartPages.get(index);
                    contents.append(linkLine(entries.get(index).name(), entries.get(index).data(), recipePage));
                }
                pages.add(page(contents));
            }

            // Build recipe pages
            for (int index = 0; index < entries.size(); index++) {
                BrewersNotebookData.Entry entry = entries.get(index);
                pages.addAll(buildRecipePages(entry));
            }
        }

        return new WrittenBookContent(Filterable.passThrough("Brewer's Notebook"), "Create Pop", 0, pages, true);
    }

    /**
     * Build one or more pages for a single recipe entry
     */
    private static List<Filterable<Component>> buildRecipePages(BrewersNotebookData.Entry entry) {
        List<Filterable<Component>> pages = new ArrayList<>();
        MutableComponent recipe = buildRecipeHeader(entry);

        String note = entry.note().trim();
        if (!note.isEmpty()) {
            recipe.append(Component.literal("\n\nNotes:\n"));
            // Split note into chunks that fit on a page
            List<String> noteChunks = splitNoteForPages(note);

            if (noteChunks.size() == 1) {
                recipe.append(Component.literal(noteChunks.get(0)));
                pages.add(page(recipe));
            } else {
                // First note chunk on main recipe page
                recipe.append(Component.literal(noteChunks.get(0)));
                pages.add(page(recipe));

                // Additional pages for continuation
                for (int i = 1; i < noteChunks.size(); i++) {
                    MutableComponent continuation = Component.literal("📍 ")
                            .withStyle(style -> style
                                    .withColor(ChatFormatting.AQUA)
                                    .withUnderlined(true)
                                    .withClickEvent(new ClickEvent(ClickEvent.Action.CHANGE_PAGE, "2")))
                            .append(Component.literal("\n\n" + entry.name() + " (continued)\n\n"))
                            .append(Component.literal(noteChunks.get(i)));
                    pages.add(page(continuation));
                }
            }
        } else {
            pages.add(page(recipe));
        }

        return pages;
    }

    private static MutableComponent buildRecipeHeader(BrewersNotebookData.Entry entry) {
        int rgb = entry.data().rgbColor();
        MutableComponent title = Component.literal(entry.name() + "\n")
                .withColor(rgb);

        MutableComponent instabilityText = Component.translatable(
                "createpop.soda.tooltip.instability",
                String.format(java.util.Locale.ROOT, "%.2f", entry.data().instability())
        ).withColor(0xFFB347); // amber/orange

        MutableComponent recipe = title
                .append(Component.literal("Instability: "))
                .append(instabilityText)
                .append(Component.literal("\n\nEffects:\n"));

        if (entry.data().effects().isEmpty()) {
            recipe.append(Component.translatable("createpop.soda.tooltip.no_effects"));
        } else {
            for (var effect : entry.data().effects()) {
                recipe.append(SodaTextHelper.formatEffect(effect)).append(Component.literal("\n"));
            }
        }

        return recipe;
    }

    /**
     * Split note text into chunks that fit reasonably on pages
     * Approximate: 250 characters per page for wrapping
     */
    private static List<String> splitNoteForPages(String note) {
        List<String> chunks = new ArrayList<>();
        int charsPerPage = 250;
        int start = 0;

        while (start < note.length()) {
            int end = Math.min(start + charsPerPage, note.length());

            // Try to find a good break point (newline or space)
            if (end < note.length()) {
                int lastNewline = note.lastIndexOf('\n', end);
                int lastSpace = note.lastIndexOf(' ', end);
                int breakPoint = Math.max(lastNewline, lastSpace);
                if (breakPoint > start) {
                    end = breakPoint;
                }
            }

            chunks.add(note.substring(start, end).trim());
            start = end;
            if (start < note.length() && note.charAt(start) == ' ') {
                start++;
            }
        }

        return chunks.isEmpty() ? List.of("") : chunks;
    }

    private static MutableComponent linkLine(String title, SodaData data, int page) {
        int rgb = data.rgbColor();
        return Component.literal(title + "\n")
                .withStyle(style -> style
                        .withColor(rgb)
                        .withUnderlined(true)
                        .withClickEvent(new ClickEvent(ClickEvent.Action.CHANGE_PAGE, String.valueOf(page))));
    }

    private static Filterable<Component> page(Component text) {
        return Filterable.passThrough(text);
    }

    private static boolean isTargetingBlock(Player player) {
        HitResult hit = player.pick(5.0D, 0.0F, false);
        return hit.getType() == HitResult.Type.BLOCK;
    }
}

