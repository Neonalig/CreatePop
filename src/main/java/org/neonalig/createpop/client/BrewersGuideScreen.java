package org.neonalig.createpop.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import org.neonalig.createpop.CreatePopConfig;
import org.neonalig.createpop.compat.create.DynamicSodaMixing;
import org.neonalig.createpop.registry.ModItems;

import java.util.ArrayList;
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
    private static final int LINK = 0xFF7F542F;
    private static final int LINK_HOVER = 0xFF4B2F1B;
    private static final int SELECTION = 0xC46B8E23;
    private static final int HOVER = 0x6A8A5A30;
    private static final int ENTRY_HEIGHT = 22;
    private static final int VISIBLE_SECTIONS = 6;

    private static int rememberedSelectedIndex;
    private static int rememberedListScroll;
    private static int rememberedDetailsScroll;

    private final List<Section> sections;
    private final List<LinkRegion> activeLinks = new ArrayList<>();

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
    private boolean jeiLinksAvailable;
    private LinkRegion hoveredLink;

    public BrewersGuideScreen() {
        super(Component.translatable("item.createpop.brewers_guide"));
        this.sections = createSections();
        this.selectedIndex = rememberedSelectedIndex;
        this.listScroll = rememberedListScroll;
        this.detailsScroll = rememberedDetailsScroll;
    }

    @Override
    protected void init() {
        clearWidgets();
        jeiLinksAvailable = queryJeiLinksAvailable();

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
    public void onClose() {
        rememberViewState();
        super.onClose();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (clickGuideLink(mouseX, mouseY)) {
            return true;
        }
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
                Component.translatable("createpop.brewers_guide.subtitle"),
                left + 118, top + 8, 0xFFD7C089, false);

        guiGraphics.drawString(font, Component.translatable("createpop.brewers_guide.chapters"), listLeft, listTop - 12, INK, false);
        Component progress = Component.translatable("createpop.brewers_guide.progress", selectedIndex + 1, sections.size());
        guiGraphics.drawString(font, progress, listLeft + listWidth - font.width(progress), listTop - 12, MUTED, false);

        renderSectionList(guiGraphics, mouseX, mouseY);
        renderSectionDetails(guiGraphics, mouseX, mouseY);

        if (hoveredLink != null) {
            guiGraphics.renderComponentTooltip(font, hoveredLinkTooltip(), mouseX, mouseY);
        }

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
            drawReadableString(guiGraphics, font.plainSubstrByWidth(section.title().getString(), listWidth - 8), listLeft + 5, rowTop + 3, titleColor);
            guiGraphics.drawString(font,
                    font.plainSubstrByWidth(section.summary().getString(), listWidth - 8),
                    listLeft + 5, rowTop + 13, MUTED, false);
        }
    }

    private void renderSectionDetails(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        Section section = selectedSection();
        hoveredLink = null;
        activeLinks.clear();
        if (section == null) {
            return;
        }

        guiGraphics.enableScissor(detailsPaneLeft + 3, detailsTop, detailsPaneLeft + detailsPaneWidth - 3, detailsContentBottom);

        int y = detailsTop - detailsScroll;
        y = drawWrappedShadowed(guiGraphics, section.title(), detailsLeft, y, detailsWidth, section.accentColor());
        y = drawWrappedAmber(guiGraphics, section.summary(), detailsLeft, y + 2, detailsWidth);
        y += 4;
        guiGraphics.fill(detailsLeft, y, detailsLeft + detailsWidth, y + 1, 0x553D2A1C);
        y += 8;

        for (GuideParagraph paragraph : section.paragraphs()) {
            y = drawParagraph(guiGraphics, paragraph, detailsLeft, y, detailsWidth, mouseX, mouseY);
        }

        guiGraphics.disableScissor();
        drawDetailsScrollbar(guiGraphics, maxDetailsScroll(section));
    }

    private int drawParagraph(GuiGraphics guiGraphics, GuideParagraph paragraph, int x, int y, int maxWidth, int mouseX, int mouseY) {
        int cursorX = x;
        int cursorY = y;
        for (GuideSegment segment : paragraph.segments()) {
            if (segment.linkAction() != LinkAction.NONE && !segment.item().isEmpty() && jeiLinksAvailable) {
                String remaining = segment.text().getString();
                while (!remaining.isEmpty()) {
                    if (cursorX > x && cursorX >= x + maxWidth) {
                        cursorX = x;
                        cursorY += 10;
                    }
                    int availableWidth = Math.max(10, (x + maxWidth) - cursorX);
                    String chunk = nextLinkChunk(remaining, availableWidth);
                    int chunkWidth = font.width(chunk);
                    boolean hovered = isPointInside(mouseX, mouseY, cursorX, cursorY, cursorX + chunkWidth, cursorY + 10);
                    int color = hovered ? LINK_HOVER : LINK;
                    guiGraphics.drawString(font, chunk, cursorX, cursorY, color, false);
                    guiGraphics.fill(cursorX, cursorY + 9, cursorX + chunkWidth, cursorY + 10, color);

                    LinkRegion region = new LinkRegion(cursorX, cursorY, cursorX + chunkWidth, cursorY + 10, segment.item(), segment.linkAction());
                    activeLinks.add(region);
                    if (hovered) {
                        hoveredLink = region;
                    }

                    remaining = trimLeadingSpace(remaining.substring(chunk.length()));
                    if (!remaining.isEmpty()) {
                        cursorX = x;
                        cursorY += 10;
                    } else {
                        cursorX += chunkWidth;
                    }
                }
                continue;
            }

            for (String token : tokenize(segment.text().getString())) {
                if (token.isEmpty()) {
                    continue;
                }
                if (" ".equals(token) && cursorX == x) {
                    continue;
                }
                int tokenWidth = font.width(token);
                if (cursorX > x && cursorX + tokenWidth > x + maxWidth) {
                    cursorX = x;
                    cursorY += 10;
                    if (" ".equals(token)) {
                        continue;
                    }
                }
                if (!" ".equals(token)) {
                    guiGraphics.drawString(font, token, cursorX, cursorY, segment.color(), false);
                }
                cursorX += tokenWidth;
            }
        }
        return cursorY + 10 + paragraph.gapAfter();
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

    private boolean clickGuideLink(double mouseX, double mouseY) {
        if (!jeiLinksAvailable) {
            return false;
        }
        for (LinkRegion region : activeLinks) {
            if (isPointInside(mouseX, mouseY, region.left(), region.top(), region.right(), region.bottom())) {
                if (minecraft != null) {
                    ItemStack linkedItem = region.item().copy();
                    LinkAction linkAction = region.linkAction();
                    rememberViewState();
                    super.onClose();
                    minecraft.execute(() -> openLinkedItemInJei(linkedItem, linkAction));
                    return true;
                }
            }
        }
        return false;
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
        int height = measureWrappedHeight(section.title(), detailsWidth);
        height += measureWrappedHeight(section.summary(), detailsWidth);
        height += 14;
        for (GuideParagraph paragraph : section.paragraphs()) {
            height += measureParagraphHeight(paragraph, detailsWidth);
        }
        return height;
    }

    private int measureParagraphHeight(GuideParagraph paragraph, int maxWidth) {
        int lines = 1;
        int cursorWidth = 0;
        for (GuideSegment segment : paragraph.segments()) {
            if (segment.linkAction() != LinkAction.NONE && !segment.item().isEmpty()) {
                String remaining = segment.text().getString();
                while (!remaining.isEmpty()) {
                    int availableWidth = Math.max(10, maxWidth - cursorWidth);
                    String chunk = nextLinkChunk(remaining, availableWidth);
                    int chunkWidth = font.width(chunk);
                    if (cursorWidth > 0 && cursorWidth + chunkWidth > maxWidth) {
                        lines++;
                        cursorWidth = 0;
                        continue;
                    }
                    remaining = trimLeadingSpace(remaining.substring(chunk.length()));
                    if (!remaining.isEmpty()) {
                        lines++;
                        cursorWidth = 0;
                    } else {
                        cursorWidth += chunkWidth;
                    }
                }
                continue;
            }

            for (String token : tokenize(segment.text().getString())) {
                if (token.isEmpty()) {
                    continue;
                }
                int tokenWidth = font.width(token);
                if (cursorWidth > 0 && cursorWidth + tokenWidth > maxWidth) {
                    lines++;
                    cursorWidth = " ".equals(token) ? 0 : tokenWidth;
                } else if (!(" ".equals(token) && cursorWidth == 0)) {
                    cursorWidth += tokenWidth;
                }
            }
        }
        return (lines * 10) + paragraph.gapAfter();
    }

    private int measureWrappedHeight(Component text, int maxWidth) {
        return font.split(text, maxWidth).size() * 10;
    }

    private int drawWrappedAmber(GuiGraphics guiGraphics, Component text, int x, int y, int maxWidth) {
        List<FormattedCharSequence> lines = font.split(text, maxWidth);
        for (FormattedCharSequence line : lines) {
            guiGraphics.drawString(font, line, x, y, 0xFFB97638, false);
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

    private void drawDetailsScrollbar(GuiGraphics guiGraphics, int maxScroll) {
        int trackLeft = detailsPaneLeft + detailsPaneWidth - 8;
        int trackTop = detailsTop;
        int trackBottom = detailsContentBottom;
        guiGraphics.fill(trackLeft, trackTop, trackLeft + 4, trackBottom, 0x221A120A);
        if (maxScroll <= 0) {
            return;
        }
        int trackHeight = trackBottom - trackTop;
        int thumbHeight = Math.max(18, (trackHeight * trackHeight) / (trackHeight + maxScroll));
        int thumbTravel = Math.max(0, trackHeight - thumbHeight);
        int thumbTop = trackTop + (thumbTravel * detailsScroll / maxScroll);
        guiGraphics.fill(trackLeft, thumbTop, trackLeft + 4, thumbTop + thumbHeight, ACCENT);
        guiGraphics.fill(trackLeft, thumbTop, trackLeft + 4, thumbTop + 1, 0x55FFFFFF);
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
        ItemStack notebook = new ItemStack(ModItems.BREWERS_NOTEBOOK.get());
        ItemStack diamond = new ItemStack(Items.DIAMOND);
        ItemStack potion = PotionContents.createItemStack(Items.POTION, Potions.POISON);
        ItemStack dye = new ItemStack(Items.RED_DYE);
        ItemStack acacia = new ItemStack(Items.STRIPPED_ACACIA_LOG);
        ItemStack magmaCream = new ItemStack(Items.MAGMA_CREAM);
        ItemStack amethyst = new ItemStack(Items.AMETHYST_SHARD);
        return List.of(
                new Section(
                        tr("createpop.brewers_guide.section.overview.title"),
                        tr("createpop.brewers_guide.section.overview.summary"),
                        0xFFC98B49,
                        List.of(
                                paragraph(text("createpop.brewers_guide.section.overview.line1", INK)),
                                paragraph(text("createpop.brewers_guide.section.overview.line2", INK, portion)),
                                paragraph(
                                        text("createpop.brewers_guide.section.overview.line3.prefix", MUTED),
                                        itemRecipeLink(notebook, notebook.getHoverName()),
                                        text("createpop.brewers_guide.section.overview.line3.suffix", MUTED)
                                )
                        )
                ),
                new Section(
                        tr("createpop.brewers_guide.section.carb.title"),
                        tr("createpop.brewers_guide.section.carb.summary"),
                        0xFF63B8D5,
                        List.of(
                                paragraph(text("createpop.brewers_guide.section.carb.line1", INK)),
                                paragraph(
                                        text("createpop.brewers_guide.section.carb.line2.prefix", INK),
                                        itemUseLink(diamond, diamond.getHoverName()),
                                        text("createpop.brewers_guide.section.carb.line2.suffix", INK)
                                ),
                                paragraph(text("createpop.brewers_guide.section.carb.line3", INK)),
                                paragraph(text("createpop.brewers_guide.section.carb.line4", MUTED))
                        )
                ),
                new Section(
                        tr("createpop.brewers_guide.section.first_soda.title"),
                        tr("createpop.brewers_guide.section.first_soda.summary"),
                        0xFF8BBF5A,
                        List.of(
                                paragraph(
                                        text("createpop.brewers_guide.section.first_soda.line1.prefix", INK),
                                        itemUseLink(potion, tr("createpop.brewers_guide.links.potion")),
                                        text("createpop.brewers_guide.section.first_soda.line1.suffix", INK)
                                ),
                                paragraph(text("createpop.brewers_guide.section.first_soda.line2", INK)),
                                paragraph(
                                        text("createpop.brewers_guide.section.first_soda.line3.prefix", MUTED),
                                        itemUseLink(dye, tr("createpop.brewers_guide.links.dyes")),
                                        text("createpop.brewers_guide.section.first_soda.line3.suffix", MUTED)
                                )
                        )
                ),
                new Section(
                        tr("createpop.brewers_guide.section.rules.title"),
                        tr("createpop.brewers_guide.section.rules.summary"),
                        0xFFB06FE0,
                        List.of(
                                paragraph(text("createpop.brewers_guide.section.rules.line1", INK)),
                                paragraph(
                                        text("createpop.brewers_guide.section.rules.line2.prefix", INK),
                                        itemUseLink(dye, tr("createpop.brewers_guide.links.dyes")),
                                        text("createpop.brewers_guide.section.rules.line2.suffix", INK)
                                ),
                                paragraph(text("createpop.brewers_guide.section.rules.line3", MUTED))
                        )
                ),
                new Section(
                        tr("createpop.brewers_guide.section.instability.title"),
                        tr("createpop.brewers_guide.section.instability.summary"),
                        0xFFD07054,
                        List.of(
                                paragraph(text("createpop.brewers_guide.section.instability.line1", INK)),
                                paragraph(
                                        text("createpop.brewers_guide.section.instability.line2.prefix", INK),
                                        itemUseLink(acacia, acacia.getHoverName()),
                                        text("createpop.brewers_guide.section.instability.line2.middle1", INK, percent(CreatePopConfig.acaciaLogInstabilityReduction())),
                                        itemUseLink(magmaCream, magmaCream.getHoverName()),
                                        text("createpop.brewers_guide.section.instability.line2.middle2", INK, percent(CreatePopConfig.magmaCreamInstabilityReduction())),
                                        itemUseLink(amethyst, amethyst.getHoverName()),
                                        text("createpop.brewers_guide.section.instability.line2.suffix", INK, percent(CreatePopConfig.amethystShardInstabilityReduction()))
                                ),
                                paragraph(text("createpop.brewers_guide.section.instability.line3", MUTED))
                        )
                ),
                new Section(
                        tr("createpop.brewers_guide.section.discovery.title"),
                        tr("createpop.brewers_guide.section.discovery.summary"),
                        0xFFE0A84F,
                        List.of(
                                paragraph(
                                        text("createpop.brewers_guide.section.discovery.line1.prefix", INK),
                                        itemRecipeLink(notebook, notebook.getHoverName()),
                                        text("createpop.brewers_guide.section.discovery.line1.suffix", INK)
                                ),
                                paragraph(text("createpop.brewers_guide.section.discovery.line2", INK)),
                                paragraph(text("createpop.brewers_guide.section.discovery.line3", MUTED))
                        )
                )
        );
    }

    private GuideParagraph paragraph(GuideSegment... segments) {
        return new GuideParagraph(List.of(segments), 4);
    }

    private GuideSegment text(String key, int color, Object... args) {
        return new GuideSegment(Component.translatable(key, args), color, ItemStack.EMPTY, LinkAction.NONE);
    }

    private GuideSegment itemRecipeLink(ItemStack stack, Component label) {
        return new GuideSegment(label.copy(), LINK, stack.copy(), LinkAction.RECIPES);
    }

    private GuideSegment itemUseLink(ItemStack stack, Component label) {
        return new GuideSegment(label.copy(), LINK, stack.copy(), LinkAction.USES);
    }

    private Component tr(String key, Object... args) {
        return Component.translatable(key, args);
    }

    private String percent(double value) {
        return String.format(Locale.ROOT, "%.0f%%", value * 100.0D);
    }

    private List<String> tokenize(String text) {
        List<String> tokens = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == ' ') {
                if (!current.isEmpty()) {
                    tokens.add(current.toString());
                    current.setLength(0);
                }
                tokens.add(" ");
            } else {
                current.append(c);
            }
        }
        if (!current.isEmpty()) {
            tokens.add(current.toString());
        }
        return tokens;
    }

    private void rememberViewState() {
        rememberedSelectedIndex = selectedIndex;
        rememberedListScroll = listScroll;
        rememberedDetailsScroll = detailsScroll;
    }

    private List<Component> hoveredLinkTooltip() {
        if (hoveredLink == null) {
            return List.of();
        }
        MutableComponent hint = Component.translatable(hoveredLink.linkAction().tooltipKey()).copy().withColor(MUTED);
        return List.of(hoveredLink.item().getHoverName(), hint);
    }

    private boolean queryJeiLinksAvailable() {
        try {
            Class<?> pluginClass = Class.forName("org.neonalig.createpop.compat.jei.CreatePopJeiPlugin");
            Object result = pluginClass.getMethod("isRuntimeAvailable").invoke(null);
            return Boolean.TRUE.equals(result);
        } catch (Throwable ignored) {
            return false;
        }
    }

    private void openLinkedItemInJei(ItemStack stack, LinkAction action) {
        try {
            Class<?> pluginClass = Class.forName("org.neonalig.createpop.compat.jei.CreatePopJeiPlugin");
            String methodName = action == LinkAction.RECIPES ? "showItemRecipes" : "showItemUses";
            pluginClass.getMethod(methodName, ItemStack.class).invoke(null, stack);
        } catch (Throwable ignored) {
        }
    }

    private String nextLinkChunk(String text, int maxWidth) {
        if (!text.contains(" ")) {
            return font.plainSubstrByWidth(text, maxWidth);
        }
        String[] words = text.split(" ");
        StringBuilder builder = new StringBuilder();
        for (String word : words) {
            String candidate = builder.isEmpty() ? word : builder + " " + word;
            if (builder.isEmpty() || font.width(candidate) <= maxWidth) {
                builder.setLength(0);
                builder.append(candidate);
            } else {
                break;
            }
        }
        if (builder.isEmpty()) {
            return font.plainSubstrByWidth(text, maxWidth);
        }
        return builder.toString();
    }

    private String trimLeadingSpace(String text) {
        int index = 0;
        while (index < text.length() && text.charAt(index) == ' ') {
            index++;
        }
        return text.substring(index);
    }

    private boolean isPointInside(double mouseX, double mouseY, int left, int top, int right, int bottom) {
        return mouseX >= left && mouseX <= right && mouseY >= top && mouseY <= bottom;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private record GuideSegment(Component text, int color, ItemStack item, LinkAction linkAction) {
    }

    private record GuideParagraph(List<GuideSegment> segments, int gapAfter) {
    }

    private record LinkRegion(int left, int top, int right, int bottom, ItemStack item, LinkAction linkAction) {
    }

    private record Section(Component title, Component summary, int accentColor, List<GuideParagraph> paragraphs) {
    }

    private enum LinkAction {
        NONE(""),
        RECIPES("createpop.brewers_guide.link_hint.recipes"),
        USES("createpop.brewers_guide.link_hint.uses");

        private final String tooltipKey;

        LinkAction(String tooltipKey) {
            this.tooltipKey = tooltipKey;
        }

        public String tooltipKey() {
            return tooltipKey;
        }
    }
}
