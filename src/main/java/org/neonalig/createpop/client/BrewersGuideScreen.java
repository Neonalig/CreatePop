package org.neonalig.createpop.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import org.neonalig.createpop.CreatePopConfig;
import org.neonalig.createpop.compat.create.DynamicSodaMixing;

import java.util.List;
import java.util.Locale;

public class BrewersGuideScreen extends Screen {
    private static final int WINDOW_WIDTH = 372;
    private static final int WINDOW_HEIGHT = 252;
    private static final int OUTER = 0xFF3A2618;
    private static final int PANEL_DARK = 0xFFD7C08C;
    private static final int PANEL_LIGHT = 0xFFF4E7C0;
    private static final int ACCENT = 0xFF8A5A30;
    private static final int INK = 0xFF2D2118;
    private static final int MUTED = 0xFF6E5B43;
    private static final int SOFT_SHADOW = 0x66000000;
    private static final int TITLE_SHADOW = 0xCC000000;
    private static final int SELECTION = 0xC46B8E23;
    private static final int HOVER = 0x6A8A5A30;
    private static final int ENTRY_HEIGHT = 22;
    private static final int VISIBLE_SECTIONS = 6;

    private final List<Section> sections;

    private int selectedIndex;
    private int listScroll;
    private int detailsScroll;
    private int left;
    private int top;
    private int listPaneLeft;
    private int listPaneTop;
    private int listPaneWidth;
    private int listPaneHeight;
    private int listLeft;
    private int listTop;
    private int listWidth;
    private int detailsPaneLeft;
    private int detailsPaneTop;
    private int detailsPaneWidth;
    private int detailsPaneHeight;
    private int detailsLeft;
    private int detailsTop;
    private int detailsWidth;
    private int detailsContentBottom;

    public BrewersGuideScreen() {
        super(Component.translatable("item.createpop.brewers_guide"));
        this.sections = createSections();
    }

    @Override
    protected void init() {
        clearWidgets();

        left = (width - WINDOW_WIDTH) / 2;
        top = (height - WINDOW_HEIGHT) / 2;

        listPaneLeft = left + 10;
        listPaneTop = top + 18;
        listPaneWidth = 132;
        listPaneHeight = WINDOW_HEIGHT - 60;
        listLeft = listPaneLeft + 8;
        int listButtonX = listPaneLeft + listPaneWidth - 26;
        listWidth = listButtonX - 4 - listLeft;
        listTop = listPaneTop + 18;

        detailsPaneLeft = listPaneLeft + listPaneWidth + 8;
        detailsPaneTop = listPaneTop;
        detailsPaneWidth = WINDOW_WIDTH - (detailsPaneLeft - left) - 10;
        detailsPaneHeight = listPaneHeight;
        detailsLeft = detailsPaneLeft + 10;
        detailsTop = detailsPaneTop + 10;
        detailsWidth = detailsPaneWidth - 20;
        detailsContentBottom = detailsPaneTop + detailsPaneHeight - 12;

        listScroll = Mth.clamp(listScroll, 0, maxListScroll());
        selectedIndex = Mth.clamp(selectedIndex, 0, Math.max(0, sections.size() - 1));
        detailsScroll = Mth.clamp(detailsScroll, 0, maxDetailsScroll(selectedSection()));

        Button upButton = Button.builder(Component.literal("↑"), button -> scrollListBy(-1))
                .bounds(listButtonX, listTop, 18, 18)
                .build();
        upButton.active = listScroll > 0;
        addRenderableWidget(upButton);

        Button downButton = Button.builder(Component.literal("↓"), button -> scrollListBy(1))
                .bounds(listButtonX, listTop + (VISIBLE_SECTIONS * ENTRY_HEIGHT) - 18, 18, 18)
                .build();
        downButton.active = listScroll < maxListScroll();
        addRenderableWidget(downButton);

        addRenderableWidget(Button.builder(Component.translatable("createpop.brewers_notebook.done"), button -> onClose())
                .bounds(left + 132, top + WINDOW_HEIGHT - 28, 108, 20)
                .build());
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (clickSection(mouseX, mouseY)) {
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int listBottom = listTop + (VISIBLE_SECTIONS * ENTRY_HEIGHT);
        if (mouseX >= listLeft && mouseX <= listLeft + listWidth && mouseY >= listTop && mouseY <= listBottom) {
            int delta = -(int) Math.signum(scrollY);
            if (delta != 0) {
                moveSelection(delta);
            }
            return true;
        }
        if (mouseX >= detailsPaneLeft + 4 && mouseX <= detailsPaneLeft + detailsPaneWidth - 4
                && mouseY >= detailsTop && mouseY <= detailsContentBottom) {
            detailsScroll = Mth.clamp(detailsScroll - ((int) Math.signum(scrollY) * 10), 0, maxDetailsScroll(selectedSection()));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics, mouseX, mouseY, partialTick);

        guiGraphics.fill(0, 0, width, height, 0x88000000);
        drawFrame(guiGraphics, left, top, WINDOW_WIDTH, WINDOW_HEIGHT, OUTER, ACCENT);
        drawFrame(guiGraphics, listPaneLeft, listPaneTop, listPaneWidth, listPaneHeight, PANEL_DARK, PANEL_LIGHT);
        drawFrame(guiGraphics, detailsPaneLeft, detailsPaneTop, detailsPaneWidth, detailsPaneHeight, PANEL_DARK, PANEL_LIGHT);

        guiGraphics.drawString(font, title, left + 14, top + 8, PANEL_LIGHT, false);
        guiGraphics.drawString(font,
                Component.literal("Carbonation, mixing, stability, and field notes."),
                left + 118, top + 8, 0xFFD7C089, false);

        guiGraphics.drawString(font, Component.literal("Chapters"), listLeft, listTop - 12, INK, false);
        guiGraphics.drawString(font,
                Component.literal(selectedIndex + 1 + "/" + sections.size()),
                listLeft + listWidth - font.width(selectedIndex + 1 + "/" + sections.size()), listTop - 12, MUTED, false);

        renderSectionList(guiGraphics, mouseX, mouseY);
        renderSectionDetails(guiGraphics);

        for (net.minecraft.client.gui.components.Renderable renderable : this.renderables) {
            renderable.render(guiGraphics, mouseX, mouseY, partialTick);
        }
    }

    private void renderSectionList(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        for (int row = 0; row < VISIBLE_SECTIONS; row++) {
            int index = listScroll + row;
            if (index >= sections.size()) {
                break;
            }
            Section section = sections.get(index);
            int rowTop = listTop + (row * ENTRY_HEIGHT);
            boolean selected = index == selectedIndex;
            boolean hovered = mouseX >= listLeft && mouseX <= listLeft + listWidth && mouseY >= rowTop && mouseY < rowTop + ENTRY_HEIGHT;
            int fill = selected ? SELECTION : hovered ? HOVER : 0x2255402B;
            guiGraphics.fill(listLeft, rowTop, listLeft + listWidth, rowTop + ENTRY_HEIGHT - 1, fill);
            guiGraphics.fill(listLeft, rowTop + ENTRY_HEIGHT - 1, listLeft + listWidth, rowTop + ENTRY_HEIGHT, 0x332D2118);

            int titleColor = selected ? 0xFFFDF5D3 : section.accentColor();
            drawReadableString(guiGraphics, font.plainSubstrByWidth(section.title(), listWidth - 8), listLeft + 5, rowTop + 3, titleColor);
            guiGraphics.drawString(font,
                    font.plainSubstrByWidth(section.summary(), listWidth - 8),
                    listLeft + 5, rowTop + 13, MUTED, false);
        }
    }

    private void renderSectionDetails(GuiGraphics guiGraphics) {
        Section section = selectedSection();
        if (section == null) {
            return;
        }

        guiGraphics.enableScissor(detailsPaneLeft + 3, detailsTop, detailsPaneLeft + detailsPaneWidth - 3, detailsContentBottom);

        int y = detailsTop - detailsScroll;
        y = drawWrappedShadowed(guiGraphics, Component.literal(section.title()), detailsLeft, y, detailsWidth, section.accentColor());
        y = drawWrapped(guiGraphics, Component.literal(section.summary()), detailsLeft, y + 2, detailsWidth, 0xFFB97638);
        y += 4;
        guiGraphics.fill(detailsLeft, y, detailsLeft + detailsWidth, y + 1, 0x553D2A1C);
        y += 8;

        for (GuideLine line : section.lines()) {
            y = drawWrapped(guiGraphics, line.text(), detailsLeft, y, detailsWidth, line.color());
            y += 4;
        }

        guiGraphics.disableScissor();
    }

    private boolean clickSection(double mouseX, double mouseY) {
        int listBottom = listTop + (VISIBLE_SECTIONS * ENTRY_HEIGHT);
        if (mouseX < listLeft || mouseX > listLeft + listWidth || mouseY < listTop || mouseY > listBottom) {
            return false;
        }
        int row = (int) ((mouseY - listTop) / ENTRY_HEIGHT);
        int index = listScroll + row;
        if (index < 0 || index >= sections.size()) {
            return false;
        }
        selectedIndex = index;
        detailsScroll = 0;
        init();
        return true;
    }

    private void scrollListBy(int delta) {
        int nextScroll = Mth.clamp(listScroll + delta, 0, maxListScroll());
        if (nextScroll == listScroll) {
            return;
        }
        listScroll = nextScroll;
        init();
    }

    private void moveSelection(int delta) {
        if (sections.isEmpty()) {
            return;
        }
        int target = Mth.clamp(selectedIndex + delta, 0, sections.size() - 1);
        if (target == selectedIndex) {
            return;
        }
        selectedIndex = target;
        detailsScroll = 0;
        if (selectedIndex < listScroll) {
            listScroll = selectedIndex;
        } else if (selectedIndex >= listScroll + VISIBLE_SECTIONS) {
            listScroll = selectedIndex - VISIBLE_SECTIONS + 1;
        }
        init();
    }

    private int maxListScroll() {
        return Math.max(0, sections.size() - VISIBLE_SECTIONS);
    }

    private int maxDetailsScroll(Section section) {
        if (section == null) {
            return 0;
        }
        return Math.max(0, measureSectionHeight(section) - (detailsContentBottom - detailsTop));
    }

    private int measureSectionHeight(Section section) {
        int height = measureWrappedHeight(Component.literal(section.title()), detailsWidth);
        height += measureWrappedHeight(Component.literal(section.summary()), detailsWidth);
        height += 14;
        for (GuideLine line : section.lines()) {
            height += measureWrappedHeight(line.text(), detailsWidth);
            height += 4;
        }
        return height;
    }

    private int measureWrappedHeight(Component text, int maxWidth) {
        return font.split(text, maxWidth).size() * 10;
    }

    private int drawWrapped(GuiGraphics guiGraphics, Component text, int x, int y, int maxWidth, int color) {
        List<FormattedCharSequence> lines = font.split(text, maxWidth);
        for (FormattedCharSequence line : lines) {
            guiGraphics.drawString(font, line, x, y, color, false);
            y += 10;
        }
        return y;
    }

    private int drawWrappedShadowed(GuiGraphics guiGraphics, Component text, int x, int y, int maxWidth, int color) {
        List<FormattedCharSequence> lines = font.split(text, maxWidth);
        for (FormattedCharSequence line : lines) {
            guiGraphics.drawString(font, line, x + 1, y + 1, TITLE_SHADOW, false);
            guiGraphics.drawString(font, line, x, y, color, false);
            y += 10;
        }
        return y;
    }

    private void drawReadableString(GuiGraphics guiGraphics, String text, int x, int y, int color) {
        guiGraphics.drawString(font, text, x + 1, y + 1, SOFT_SHADOW, false);
        guiGraphics.drawString(font, text, x, y, color, false);
    }

    private void drawFrame(GuiGraphics guiGraphics, int x, int y, int width, int height, int border, int fill) {
        guiGraphics.fill(x, y, x + width, y + height, border);
        guiGraphics.fill(x + 2, y + 2, x + width - 2, y + height - 2, fill);
        guiGraphics.fill(x + 3, y + 3, x + width - 3, y + 5, 0x30FFFFFF);
        guiGraphics.fill(x + 3, y + height - 5, x + width - 3, y + height - 3, 0x40000000);
    }

    private Section selectedSection() {
        if (sections.isEmpty()) {
            return null;
        }
        return sections.get(Mth.clamp(selectedIndex, 0, sections.size() - 1));
    }

    private List<Section> createSections() {
        String portion = DynamicSodaMixing.DRINK_AMOUNT + " mB";
        return List.of(
                new Section(
                        "Overview",
                        "What this guide is for",
                        0xFFC98B49,
                        List.of(
                                line("Create Pop revolves around turning plain water into carbonated water, then combining that base with potions, sodas, dyes, or stabilisers in a Create mixer.", INK),
                                line("Every proper mix works in drink-sized portions of " + portion + ", so aim to feed the basin with at least that much of each fluid input.", INK),
                                line("Use this guide as a field manual: the notebook is where you keep your personal discoveries and tasting notes once you start experimenting.", MUTED)
                        )
                ),
                new Section(
                        "Carbonation",
                        "Start with carbonated water",
                        0xFF63B8D5,
                        List.of(
                                line("1. Put water into a Create mixer basin.", INK),
                                line("2. Add a diamond to carbonate it.", INK),
                                line("3. Bottle, bucket, or pipe the result if you want to transport it before the next mix.", INK),
                                line("Carbonated water is the backbone for every first-generation soda. If a recipe feels like it should work but refuses to mix, check that you are starting from carbonated water instead of plain water.", MUTED)
                        )
                ),
                new Section(
                        "First Soda",
                        "Turning potions into soda bases",
                        0xFF8BBF5A,
                        List.of(
                                line("Mix carbonated water with a beneficial tier-1 potion to create a soda base. The potion can be supplied as a fluid or as an item, depending on how you are processing it.", INK),
                                line("The resulting soda inherits reduced, drinkable effect timings and gains a color that helps identify it at a glance.", INK),
                                line("Once you have a base soda, you can remix it with other sodas, tint it with dyes, or stabilise it if the recipe starts getting unruly.", MUTED)
                        )
                ),
                new Section(
                        "Mixing Rules",
                        "Combining flavors and effects",
                        0xFFB06FE0,
                        List.of(
                                line("Mixing two different sodas combines their effects into a new drink. Matching effects tend to stack duration, while wildly uneven amounts lead to more dilution and shorter results.", INK),
                                line("Dyes recolor an existing soda without changing its core effects, which is useful for theming batches or marking stronger blends.", INK),
                                line("Try to mix deliberately rather than randomly: once two inputs are the same exact soda, the mixer intentionally avoids wasting them on a pointless same-for-same combine.", MUTED)
                        )
                ),
                new Section(
                        "Instability",
                        "Why risky blends bite back",
                        0xFFD07054,
                        List.of(
                                line("Every remix adds instability. The more complicated the drink becomes, the more likely it is to pick up drawbacks or otherwise drift away from the clean result you expected.", INK),
                                line("Use stripped acacia logs in a heated mixer to shave off about " + percent(CreatePopConfig.acaciaLogInstabilityReduction()) + " instability, magma cream in a heated mixer for about " + percent(CreatePopConfig.magmaCreamInstabilityReduction()) + ", or an amethyst shard in a superheated mixer for about " + percent(CreatePopConfig.amethystShardInstabilityReduction()) + ".", INK),
                                line("A little instability can be manageable, but if you are chasing polished recipes, stabilise between major remix steps instead of waiting until the very end.", MUTED)
                        )
                ),
                new Section(
                        "Discovery Loop",
                        "Learn, record, refine",
                        0xFFE0A84F,
                        List.of(
                                line("The brewer's notebook is your long-term memory. Save learned recipes into it, reopen them later, and write down what made a batch good, bad, or worth revisiting.", INK),
                                line("When you discover something new, name it clearly and note what the drink felt like in practice: effect mix, instability, taste theme, and whether it is worth mass production.", INK),
                                line("The best soda lab is iterative: discover a base, record it, refine it, and only then commit it to your regular production line.", MUTED)
                        )
                )
        );
    }

    private GuideLine line(String text, int color) {
        return new GuideLine(Component.literal(text), color);
    }

    private String percent(double value) {
        return String.format(Locale.ROOT, "%.0f%%", value * 100.0D);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private record GuideLine(Component text, int color) {
    }

    private record Section(String title, String summary, int accentColor, List<GuideLine> lines) {
    }
}

