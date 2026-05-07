package org.neonalig.createpop.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.network.PacketDistributor;
import org.neonalig.createpop.component.BrewersNotebookData;
import org.neonalig.createpop.network.RemoveNotebookEntryPayload;
import org.neonalig.createpop.network.UpdateNotebookNotePayload;
import org.neonalig.createpop.soda.SodaTextHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class BrewersNotebookScreen extends Screen {
    private static final int WINDOW_WIDTH = 372;
    private static final int WINDOW_HEIGHT = 228;
    private static final int OUTER = 0xFF3A2618;
    private static final int PANEL_DARK = 0xFFD7C08C;
    private static final int PANEL_LIGHT = 0xFFF4E7C0;
    private static final int ACCENT = 0xFF8A5A30;
    private static final int INK = 0xFF2D2118;
    private static final int MUTED = 0xFF6E5B43;
    private static final int SELECTION = 0xC46B8E23;
    private static final int HOVER = 0x6A8A5A30;
    private static final int ENTRY_HEIGHT = 20;
    private static final int VISIBLE_ENTRIES = 8;
    private static final int NOTE_LINES_PER_PAGE = 6;
    private static final int MAX_NOTE_PAGES = 8;

    private final boolean mainHand;
    private final List<BrewersNotebookData.Entry> allEntries;
    private final List<BrewersNotebookData.Entry> filteredEntries = new ArrayList<>();
    private final List<String> editableNoteLines = new ArrayList<>();
    private final List<EditBox> noteEditors = new ArrayList<>();

    private EditBox searchBox;
    private String selectedKey;
    private String loadedNoteKey;
    private int listScroll;
    private int notePage;

    private int left;
    private int top;
    private int listLeft;
    private int listTop;
    private int listWidth;
    private int detailsLeft;
    private int detailsTop;
    private int detailsWidth;
    private int noteEditorsTop;
    private int noteEditorsWidth;

    public BrewersNotebookScreen(BrewersNotebookData notebookData, boolean mainHand) {
        super(Component.translatable("item.createpop.brewers_notebook"));
        this.mainHand = mainHand;
        this.allEntries = new ArrayList<>(notebookData.entries());
        if (!this.allEntries.isEmpty()) {
            this.selectedKey = this.allEntries.getFirst().key();
        }
        rebuildFilter("");
    }

    @Override
    protected void init() {
        syncVisibleNoteEditors();
        clearWidgets();
        noteEditors.clear();

        left = (width - WINDOW_WIDTH) / 2;
        top = (height - WINDOW_HEIGHT) / 2;
        listLeft = left + 14;
        listTop = top + 48;
        listWidth = 128;
        detailsLeft = left + 154;
        detailsTop = top + 18;
        detailsWidth = WINDOW_WIDTH - 170;
        noteEditorsTop = top + 137;
        noteEditorsWidth = detailsWidth - 22;

        String existingSearch = searchBox == null ? "" : searchBox.getValue();
        searchBox = new EditBox(font, listLeft, top + 24, listWidth - 22, 18,
                Component.translatable("createpop.brewers_notebook.search"));
        searchBox.setValue(existingSearch);
        searchBox.setResponder(this::onSearchChanged);
        addRenderableWidget(searchBox);

        addRenderableWidget(Button.builder(Component.literal("×"), button -> searchBox.setValue(""))
                .bounds(listLeft + listWidth - 18, top + 24, 18, 18)
                .build());

        addRenderableWidget(Button.builder(Component.literal("↑"), button -> listScroll = Math.max(0, listScroll - 1))
                .bounds(listLeft + listWidth - 18, listTop, 18, 18)
                .build());
        addRenderableWidget(Button.builder(Component.literal("↓"), button -> listScroll = Math.min(maxListScroll(), listScroll + 1))
                .bounds(listLeft + listWidth - 18, listTop + (VISIBLE_ENTRIES * ENTRY_HEIGHT) - 18, 18, 18)
                .build());

        addRenderableWidget(Button.builder(Component.translatable("createpop.brewers_notebook.done"), button -> onClose())
                .bounds(left + WINDOW_WIDTH - 70, top + WINDOW_HEIGHT - 24, 56, 18)
                .build());

        BrewersNotebookData.Entry selected = selectedEntry();
        if (selected != null) {
            ensureEditableNoteMatches(selected);
            addRenderableWidget(Button.builder(Component.literal("<"), button -> changeNotePage(-1))
                    .bounds(detailsLeft + detailsWidth - 50, top + 114, 20, 18)
                    .build());
            addRenderableWidget(Button.builder(Component.literal(">"), button -> changeNotePage(1))
                    .bounds(detailsLeft + detailsWidth - 24, top + 114, 20, 18)
                    .build());

            addRenderableWidget(Button.builder(Component.translatable("createpop.brewers_notebook.save_note"), button -> saveSelectedNote())
                    .bounds(detailsLeft, top + WINDOW_HEIGHT - 24, 70, 18)
                    .build());
            addRenderableWidget(Button.builder(Component.translatable("createpop.brewers_notebook.delete_entry"), button -> deleteSelectedEntry())
                    .bounds(detailsLeft + 76, top + WINDOW_HEIGHT - 24, 78, 18)
                    .build());

            createNoteEditors();
        }

        setInitialFocus(searchBox);
    }

    private void createNoteEditors() {
        noteEditors.clear();
        ensureEditableCapacity((notePage + 1) * NOTE_LINES_PER_PAGE);
        int lineHeight = 16;
        for (int line = 0; line < NOTE_LINES_PER_PAGE; line++) {
            int index = (notePage * NOTE_LINES_PER_PAGE) + line;
            EditBox editor = new EditBox(font, detailsLeft + 8, noteEditorsTop + (line * 18), noteEditorsWidth, lineHeight,
                    Component.translatable("createpop.brewers_notebook.note_line"));
            editor.setMaxLength(120);
            editor.setBordered(false);
            editor.setTextColor(INK);
            editor.setTextColorUneditable(INK);
            editor.setValue(index < editableNoteLines.size() ? editableNoteLines.get(index) : "");
            noteEditors.add(editor);
            addRenderableWidget(editor);
        }
    }

    private void rebuildFilter(String query) {
        String previousSelection = selectedKey;
        filteredEntries.clear();
        String needle = query.toLowerCase(Locale.ROOT).trim();
        for (BrewersNotebookData.Entry entry : allEntries) {
            if (needle.isEmpty() || matchesSearch(entry, needle)) {
                filteredEntries.add(entry);
            }
        }

        listScroll = Mth.clamp(listScroll, 0, maxListScroll());
        if (selectedKey == null || filteredEntries.stream().noneMatch(entry -> entry.key().equals(selectedKey))) {
            selectedKey = filteredEntries.isEmpty() ? null : filteredEntries.getFirst().key();
            notePage = 0;
        }
        if ((previousSelection == null && selectedKey != null)
                || (previousSelection != null && !previousSelection.equals(selectedKey))) {
            loadedNoteKey = null;
        }
    }

    private void onSearchChanged(String query) {
        rebuildFilter(query);
        init();
    }

    private boolean matchesSearch(BrewersNotebookData.Entry entry, String needle) {
        if (entry.name().toLowerCase(Locale.ROOT).contains(needle)) {
            return true;
        }
        for (String ingredient : entry.ingredients()) {
            if (ingredient.toLowerCase(Locale.ROOT).contains(needle)) {
                return true;
            }
        }
        return false;
    }

    private BrewersNotebookData.Entry selectedEntry() {
        if (selectedKey == null) {
            return null;
        }
        for (BrewersNotebookData.Entry entry : allEntries) {
            if (entry.key().equals(selectedKey)) {
                return entry;
            }
        }
        return null;
    }

    private void ensureEditableNoteMatches(BrewersNotebookData.Entry entry) {
        if (entry.key().equals(loadedNoteKey)) {
            return;
        }
        editableNoteLines.clear();
        editableNoteLines.addAll(List.of(entry.note().split("\\n", -1)));
        if (editableNoteLines.isEmpty()) {
            editableNoteLines.add("");
        }
        loadedNoteKey = entry.key();
        notePage = Mth.clamp(notePage, 0, maxNotePage());
    }

    private void ensureEditableCapacity(int size) {
        while (editableNoteLines.size() < size) {
            editableNoteLines.add("");
        }
    }

    private void syncVisibleNoteEditors() {
        if (noteEditors.isEmpty()) {
            return;
        }
        ensureEditableCapacity((notePage + 1) * NOTE_LINES_PER_PAGE);
        for (int line = 0; line < noteEditors.size(); line++) {
            editableNoteLines.set((notePage * NOTE_LINES_PER_PAGE) + line, noteEditors.get(line).getValue());
        }
    }

    private void changeNotePage(int delta) {
        syncVisibleNoteEditors();
        int target = notePage + delta;
        if (delta > 0 && target >= MAX_NOTE_PAGES) {
            return;
        }
        if (delta > 0 && target > maxNotePage()) {
            ensureEditableCapacity((target + 1) * NOTE_LINES_PER_PAGE);
        }
        notePage = Mth.clamp(target, 0, MAX_NOTE_PAGES - 1);
        init();
    }

    private int maxNotePage() {
        return Math.max(0, (editableNoteLines.size() - 1) / NOTE_LINES_PER_PAGE);
    }

    private void saveSelectedNote() {
        BrewersNotebookData.Entry selected = selectedEntry();
        if (selected == null) {
            return;
        }
        syncVisibleNoteEditors();
        String note = joinedNote();
        PacketDistributor.sendToServer(new UpdateNotebookNotePayload(mainHand, selected.key(), note));
        replaceEntry(new BrewersNotebookData.Entry(selected.key(), selected.data(), selected.name(), selected.ingredients(), note));
        loadedNoteKey = selected.key();
        rebuildFilter(searchBox.getValue());
        init();
    }

    private String joinedNote() {
        int last = editableNoteLines.size() - 1;
        while (last >= 0 && editableNoteLines.get(last).isBlank()) {
            last--;
        }
        if (last < 0) {
            return "";
        }
        return String.join("\n", editableNoteLines.subList(0, last + 1));
    }

    private void replaceEntry(BrewersNotebookData.Entry updated) {
        for (int i = 0; i < allEntries.size(); i++) {
            if (allEntries.get(i).key().equals(updated.key())) {
                allEntries.set(i, updated);
                return;
            }
        }
    }

    private void deleteSelectedEntry() {
        BrewersNotebookData.Entry selected = selectedEntry();
        if (selected == null) {
            return;
        }
        PacketDistributor.sendToServer(new RemoveNotebookEntryPayload(mainHand, selected.key()));
        allEntries.removeIf(entry -> entry.key().equals(selected.key()));
        editableNoteLines.clear();
        selectedKey = null;
        loadedNoteKey = null;
        rebuildFilter(searchBox.getValue());
        init();
    }

    private int maxListScroll() {
        return Math.max(0, filteredEntries.size() - VISIBLE_ENTRIES);
    }

    @Override
    public void tick() {
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (clickEntry(mouseX, mouseY)) {
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean clickEntry(double mouseX, double mouseY) {
        int listBottom = listTop + (VISIBLE_ENTRIES * ENTRY_HEIGHT);
        if (mouseX < listLeft || mouseX > listLeft + listWidth - 22 || mouseY < listTop || mouseY > listBottom) {
            return false;
        }
        int row = (int) ((mouseY - listTop) / ENTRY_HEIGHT);
        int index = listScroll + row;
        if (index < 0 || index >= filteredEntries.size()) {
            return false;
        }
        syncVisibleNoteEditors();
        selectedKey = filteredEntries.get(index).key();
        editableNoteLines.clear();
        loadedNoteKey = null;
        notePage = 0;
        init();
        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int listBottom = listTop + (VISIBLE_ENTRIES * ENTRY_HEIGHT);
        if (mouseX >= listLeft && mouseX <= listLeft + listWidth && mouseY >= listTop && mouseY <= listBottom) {
            listScroll = Mth.clamp(listScroll - (int) Math.signum(scrollY), 0, maxListScroll());
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // 1. Render the blurred world background once, then draw our opaque panels on top.
        //    We must NOT call super.render() after our panels because Screen.render() calls
        //    renderBackground() internally, which would redraw the blur over everything.
        renderBackground(guiGraphics, mouseX, mouseY, partialTick);

        // 2. Draw opaque window chrome
        guiGraphics.fill(0, 0, width, height, 0x88000000);
        drawFrame(guiGraphics, left, top, WINDOW_WIDTH, WINDOW_HEIGHT, OUTER, ACCENT);
        drawFrame(guiGraphics, listLeft - 6, top + 18, listWidth + 6, WINDOW_HEIGHT - 38, PANEL_DARK, PANEL_LIGHT);
        drawFrame(guiGraphics, detailsLeft - 8, top + 18, detailsWidth + 12, WINDOW_HEIGHT - 38, PANEL_DARK, PANEL_LIGHT);

        guiGraphics.drawString(font, title, left + 14, top + 8, PANEL_LIGHT, false);
        guiGraphics.drawString(font, Component.translatable("createpop.brewers_notebook.subtitle", allEntries.size()), left + 126, top + 8, 0xFFD7C089, false);

        guiGraphics.drawString(font, Component.translatable("createpop.brewers_notebook.search"), listLeft, top + 14, INK, false);
        guiGraphics.drawString(font, Component.translatable("createpop.brewers_notebook.recipes"), listLeft, listTop - 12, INK, false);
        guiGraphics.drawString(font, Component.translatable("createpop.brewers_notebook.results", filteredEntries.size()), listLeft + 62, listTop - 12, MUTED, false);

        renderEntryList(guiGraphics, mouseX, mouseY);
        renderDetails(guiGraphics);

        // 3. Render widgets (buttons, editboxes) on top of panels
        for (net.minecraft.client.gui.components.Renderable renderable : this.renderables) {
            renderable.render(guiGraphics, mouseX, mouseY, partialTick);
        }
    }

    private void renderEntryList(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (filteredEntries.isEmpty()) {
            drawWrapped(guiGraphics, Component.translatable("createpop.brewers_notebook.no_results"), listLeft, listTop + 8, listWidth - 24, MUTED);
            return;
        }

        for (int row = 0; row < VISIBLE_ENTRIES; row++) {
            int index = listScroll + row;
            if (index >= filteredEntries.size()) {
                break;
            }
            BrewersNotebookData.Entry entry = filteredEntries.get(index);
            int rowTop = listTop + (row * ENTRY_HEIGHT);
            boolean selected = entry.key().equals(selectedKey);
            boolean hovered = mouseX >= listLeft && mouseX <= listLeft + listWidth - 22 && mouseY >= rowTop && mouseY < rowTop + ENTRY_HEIGHT;
            int fill = selected ? SELECTION : hovered ? HOVER : 0x2255402B;
            guiGraphics.fill(listLeft, rowTop, listLeft + listWidth - 22, rowTop + ENTRY_HEIGHT - 1, fill);
            guiGraphics.fill(listLeft, rowTop + ENTRY_HEIGHT - 1, listLeft + listWidth - 22, rowTop + ENTRY_HEIGHT, 0x332D2118);

            int textColor = selected ? 0xFFFDF5D3 : entry.data().rgbColor();
            String name = font.plainSubstrByWidth(entry.name(), listWidth - 32);
            guiGraphics.drawString(font, name, listLeft + 5, rowTop + 3, textColor, false);
            guiGraphics.drawString(font,
                    Component.translatable("createpop.brewers_notebook.effects_count", entry.data().effects().size()),
                    listLeft + 5, rowTop + 12, MUTED, false);
        }
    }

    private void renderDetails(GuiGraphics guiGraphics) {
        BrewersNotebookData.Entry selected = selectedEntry();
        if (selected == null) {
            drawWrapped(guiGraphics, Component.translatable("createpop.brewers_notebook.empty_selection"), detailsLeft, detailsTop + 12, detailsWidth - 10, MUTED);
            return;
        }

        int y = detailsTop;
        y = drawWrapped(guiGraphics, Component.literal(selected.name()).withStyle(style -> style.withColor(selected.data().rgbColor())),
                detailsLeft, y, detailsWidth - 10, selected.data().rgbColor());

        guiGraphics.drawString(font,
                Component.translatable("createpop.brewers_notebook.instability_line", String.format(Locale.ROOT, "%.2f", selected.data().instability())),
                detailsLeft, y + 4, 0xFFB97638, false);
        y += 18;

        guiGraphics.drawString(font, Component.translatable("createpop.brewers_notebook.ingredients"), detailsLeft, y, INK, false);
        y += 12;
        if (selected.ingredients().isEmpty()) {
            guiGraphics.drawString(font, Component.translatable("createpop.brewers_notebook.no_ingredients"), detailsLeft + 6, y, MUTED, false);
            y += 12;
        } else {
            for (String ingredient : selected.ingredients()) {
                y = drawWrapped(guiGraphics, Component.literal("• " + ingredient), detailsLeft + 4, y, detailsWidth - 18, INK);
            }
        }

        guiGraphics.drawString(font, Component.translatable("createpop.brewers_notebook.effects"), detailsLeft, y + 2, INK, false);
        y += 14;
        if (selected.data().effects().isEmpty()) {
            guiGraphics.drawString(font, Component.translatable("createpop.soda.tooltip.no_effects"), detailsLeft + 6, y, MUTED, false);
        } else {
            for (var effect : selected.data().effects()) {
                y = drawWrapped(guiGraphics, SodaTextHelper.formatEffect(effect).copy().withStyle(ChatFormatting.DARK_GRAY), detailsLeft + 4, y, detailsWidth - 18, MUTED);
            }
        }

        guiGraphics.drawString(font, Component.translatable("createpop.brewers_notebook.notes"), detailsLeft, top + 120, INK, false);
        guiGraphics.drawString(font,
                Component.translatable("createpop.brewers_notebook.page_indicator", notePage + 1, Math.max(1, maxNotePage() + 1)),
                detailsLeft + 64, top + 120, MUTED, false);

        int noteBoxLeft = detailsLeft + 4;
        int noteBoxTop = noteEditorsTop - 4;
        int noteBoxBottom = noteEditorsTop + (NOTE_LINES_PER_PAGE * 18) - 2;
        guiGraphics.fill(noteBoxLeft, noteBoxTop, noteBoxLeft + noteEditorsWidth + 8, noteBoxBottom, 0x30FFFFFF);
        for (int i = 0; i < NOTE_LINES_PER_PAGE; i++) {
            int lineY = noteEditorsTop + (i * 18) + 12;
            guiGraphics.fill(noteBoxLeft + 4, lineY, noteBoxLeft + noteEditorsWidth + 2, lineY + 1, 0x553D2A1C);
        }
    }

    private int drawWrapped(GuiGraphics guiGraphics, Component text, int x, int y, int maxWidth, int color) {
        List<net.minecraft.util.FormattedCharSequence> lines = font.split(text, maxWidth);
        for (net.minecraft.util.FormattedCharSequence line : lines) {
            guiGraphics.drawString(font, line, x, y, color, false);
            y += 10;
        }
        return y;
    }

    private void drawFrame(GuiGraphics guiGraphics, int x, int y, int width, int height, int border, int fill) {
        guiGraphics.fill(x, y, x + width, y + height, border);
        guiGraphics.fill(x + 2, y + 2, x + width - 2, y + height - 2, fill);
        guiGraphics.fill(x + 3, y + 3, x + width - 3, y + 5, 0x30FFFFFF);
        guiGraphics.fill(x + 3, y + height - 5, x + width - 3, y + height - 3, 0x40000000);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}


