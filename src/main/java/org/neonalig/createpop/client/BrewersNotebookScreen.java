package org.neonalig.createpop.client;

import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.network.PacketDistributor;
import org.neonalig.createpop.component.BrewersNotebookData;
import org.neonalig.createpop.compat.jei.CreatePopJeiPlugin;
import org.neonalig.createpop.network.RemoveNotebookEntryPayload;
import org.neonalig.createpop.network.UpdateNotebookNotePayload;
import org.neonalig.createpop.soda.SodaTextHelper;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class BrewersNotebookScreen extends Screen {
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
    private static final int ENTRY_HEIGHT = 20;
    private static final int VISIBLE_ENTRIES = 6;
    private static final int NOTE_LINES_PER_PAGE = 3;
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
    private boolean confirmingDelete;

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
    private int detailsScroll;
    private int notePrevButtonX;
    private int notesHeaderY;
    private int noteBoxLeft;
    private int noteBoxTop;
    private int noteBoxWidth;
    private int noteEditorsTop;
    private int noteEditorsLeft;
    private int noteEditorsWidth;
    private float uiScale = 1.0F;
    private int renderLeft;
    private int renderTop;
    private boolean openSoundPlayed;
    private boolean jeiRecipeLinksAvailable;
    private int titleLinkLeft;
    private int titleLinkTop;
    private int titleLinkRight;
    private int titleLinkBottom;
    private boolean hoveredTitleLink;

    private Button saveButton;
    private Button closeButton;

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
        jeiRecipeLinksAvailable = queryJeiRecipeLinksAvailable();

        uiScale = Math.min(1.0F, Math.min((width - 8.0F) / WINDOW_WIDTH, (height - 8.0F) / WINDOW_HEIGHT));
        if (!Float.isFinite(uiScale) || uiScale <= 0.0F) {
            uiScale = 1.0F;
        }
        renderLeft = Math.round((width - (WINDOW_WIDTH * uiScale)) / 2.0F);
        renderTop = Math.round((height - (WINDOW_HEIGHT * uiScale)) / 2.0F);
        left = 0;
        top = 0;
        listPaneLeft = left + 10;
        listPaneTop = top + 18;
        listPaneWidth = 136;
        listPaneHeight = WINDOW_HEIGHT - 60;
        listLeft = listPaneLeft + 8;
        int listButtonX = listPaneLeft + listPaneWidth - 26;
        int listContentRight = listButtonX - 4;
        listWidth = listContentRight - listLeft;
        int searchLabelY = listPaneTop + 8;
        int searchBoxY = searchLabelY + 12;
        int searchBoxHeight = 14;
        int listHeaderY = searchBoxY + searchBoxHeight + 8;
        listTop = listHeaderY + 12;

        detailsPaneLeft = listPaneLeft + listPaneWidth + 8;
        detailsPaneTop = listPaneTop;
        detailsPaneWidth = WINDOW_WIDTH - (detailsPaneLeft - left) - 10;
        detailsPaneHeight = listPaneHeight;
        detailsLeft = detailsPaneLeft + 10;
        detailsTop = detailsPaneTop + 10;
        detailsWidth = detailsPaneWidth - 20;
        int footerButtonY = top + WINDOW_HEIGHT - 28;
        notesHeaderY = detailsPaneTop + detailsPaneHeight - 80;
        int noteButtonY = notesHeaderY - 4;
        notePrevButtonX = detailsPaneLeft + detailsPaneWidth - 48;
        int noteNextButtonX = detailsPaneLeft + detailsPaneWidth - 24;
        noteBoxLeft = detailsPaneLeft + 8;
        noteBoxWidth = detailsPaneWidth - 16;
        noteBoxTop = notesHeaderY + 14;
        noteEditorsTop = noteBoxTop + 4;
        noteEditorsLeft = noteBoxLeft + 6;
        noteEditorsWidth = noteBoxWidth - 12;
        detailsContentBottom = notesHeaderY - 8;
        saveButton = null;
        closeButton = null;

        String existingSearch = searchBox == null ? "" : searchBox.getValue();
        boolean hasSearch = !existingSearch.isBlank();
        int searchBoxRight = hasSearch ? listButtonX - 4 : listPaneLeft + listPaneWidth - 8;
        searchBox = new EditBox(font, listLeft, searchBoxY, searchBoxRight - listLeft, searchBoxHeight,
                Component.translatable("createpop.brewers_notebook.search"));
        searchBox.setValue(existingSearch);
        searchBox.setMaxLength(50);
        searchBox.setBordered(false);
        searchBox.setTextColor(INK);
        searchBox.setTextColorUneditable(INK);
        searchBox.setResponder(this::onSearchChanged);
        addRenderableWidget(searchBox);

        if (hasSearch) {
            addRenderableWidget(Button.builder(Component.literal("×"), button -> searchBox.setValue(""))
                    .bounds(listButtonX, searchBoxY - 1, 16, 16)
                    .build());
        }

        Button upButton = Button.builder(Component.literal("↑"), button -> scrollListBy(-1))
                .bounds(listButtonX, listTop, 18, 18)
                .build();
        upButton.active = listScroll > 0;
        addRenderableWidget(upButton);

        Button downButton = Button.builder(Component.literal("↓"), button -> scrollListBy(1))
                .bounds(listButtonX, listTop + (VISIBLE_ENTRIES * ENTRY_HEIGHT) - 18, 18, 18)
                .build();
        downButton.active = listScroll < maxListScroll();
        addRenderableWidget(downButton);

        BrewersNotebookData.Entry selected = selectedEntry();
        if (selected != null) {
            ensureEditableNoteMatches(selected);
        }
        detailsScroll = Mth.clamp(detailsScroll, 0, maxDetailsScroll(selected));
        boolean hasPendingChanges = hasPendingChanges();

        if (confirmingDelete && selected != null) {
            Button confirmLabel = Button.builder(Component.literal("Are you sure?"), button -> {
            }).bounds(left + 18, footerButtonY, 108, 20).build();
            confirmLabel.active = false;
            addRenderableWidget(confirmLabel);

            addRenderableWidget(Button.builder(Component.translatable("createpop.brewers_notebook.delete_entry"), button -> deleteSelectedEntry())
                    .bounds(left + 246, footerButtonY, 108, 20)
                    .build());

            addRenderableWidget(Button.builder(Component.literal("Cancel"), button -> cancelDeleteConfirmation())
                    .bounds(left + 132, footerButtonY, 108, 20)
                    .build());
        } else {
            saveButton = Button.builder(Component.translatable("createpop.brewers_notebook.save_note"), button -> saveSelectedNote())
                    .bounds(left + 18, footerButtonY, 108, 20)
                    .build();
            saveButton.active = selected != null && hasPendingChanges;
            addRenderableWidget(saveButton);

            Button deleteButton = Button.builder(Component.translatable("createpop.brewers_notebook.delete_entry"), button -> requestDeleteConfirmation())
                    .bounds(left + 132, footerButtonY, 108, 20)
                    .build();
            deleteButton.active = selected != null;
            addRenderableWidget(deleteButton);

            closeButton = Button.builder(hasPendingChanges
                            ? Component.literal("Discard")
                            : Component.translatable("createpop.brewers_notebook.done"),
                    button -> onClose())
                    .bounds(left + 246, footerButtonY, 108, 20)
                    .build();
            addRenderableWidget(closeButton);
        }

        Button previousPageButton = Button.builder(Component.literal("<"), button -> changeNotePage(-1))
                .bounds(notePrevButtonX, noteButtonY, 20, 18)
                .build();
        previousPageButton.active = selected != null && notePage > 0;
        addRenderableWidget(previousPageButton);

        Button nextPageButton = Button.builder(Component.literal(">"), button -> changeNotePage(1))
                .bounds(noteNextButtonX, noteButtonY, 20, 18)
                .build();
        nextPageButton.active = selected != null && notePage < (MAX_NOTE_PAGES - 1);
        addRenderableWidget(nextPageButton);

        if (selected != null) {
            createNoteEditors();
        }

        setInitialFocus(searchBox);
    }

    private void createNoteEditors() {
        noteEditors.clear();
        ensureEditableCapacity((notePage + 1) * NOTE_LINES_PER_PAGE);
        int lineHeight = 14;
        for (int line = 0; line < NOTE_LINES_PER_PAGE; line++) {
            int index = (notePage * NOTE_LINES_PER_PAGE) + line;
            EditBox editor = new EditBox(font, noteEditorsLeft, noteEditorsTop + (line * 18), noteEditorsWidth, lineHeight,
                    Component.translatable("createpop.brewers_notebook.note_line"));
            editor.setMaxLength(120);
            editor.setBordered(false);
            editor.setTextColor(INK);
            editor.setTextColorUneditable(INK);
            editor.setTextShadow(false);
            editor.setResponder(value -> refreshFooterButtons());
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
            detailsScroll = 0;
        }
    }

    private void onSearchChanged(String query) {
        confirmingDelete = false;
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
        playLocalSound("item.book.page_turn", 0.9F, 1.0F);
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
        confirmingDelete = false;
        playLocalSound("entity.villager.work_cartographer", 0.8F, 1.0F);
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
        detailsScroll = 0;
        confirmingDelete = false;
        playLocalSound("entity.villager.work_cartographer", 0.7F, 0.85F);
        rebuildFilter(searchBox.getValue());
        init();
    }

    private void requestDeleteConfirmation() {
        if (selectedEntry() == null) {
            return;
        }
        confirmingDelete = true;
        init();
    }

    private void cancelDeleteConfirmation() {
        if (!confirmingDelete) {
            return;
        }
        confirmingDelete = false;
        init();
    }

    private void refreshFooterButtons() {
        if (saveButton == null || closeButton == null) {
            return;
        }
        boolean hasPendingChanges = hasPendingChanges();
        saveButton.active = selectedEntry() != null && hasPendingChanges;
        closeButton.setMessage(hasPendingChanges
                ? Component.literal("Discard")
                : Component.translatable("createpop.brewers_notebook.done"));
    }

    private int maxListScroll() {
        return Math.max(0, filteredEntries.size() - VISIBLE_ENTRIES);
    }

    @Override
    public void tick() {
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        double localMouseX = toLocalX(mouseX);
        double localMouseY = toLocalY(mouseY);
        if (clickTitleRecipeLink(localMouseX, localMouseY)) {
            return true;
        }
        if (clickEntry(localMouseX, localMouseY)) {
            return true;
        }
        return super.mouseClicked(localMouseX, localMouseY, button);
    }

    private boolean clickEntry(double mouseX, double mouseY) {
        int listBottom = listTop + (VISIBLE_ENTRIES * ENTRY_HEIGHT);
        if (mouseX < listLeft || mouseX > listLeft + listWidth || mouseY < listTop || mouseY > listBottom) {
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
        detailsScroll = 0;
        confirmingDelete = false;
        playLocalSound("item.book.page_turn", 0.9F, 1.0F);
        init();
        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        double localMouseX = toLocalX(mouseX);
        double localMouseY = toLocalY(mouseY);
        int listBottom = listTop + (VISIBLE_ENTRIES * ENTRY_HEIGHT);
        if (localMouseX >= listLeft && localMouseX <= listLeft + listWidth && localMouseY >= listTop && localMouseY <= listBottom) {
            int delta = -(int) Math.signum(scrollY);
            if (delta != 0) {
                moveListSelection(delta);
            }
            return true;
        }
        BrewersNotebookData.Entry selected = selectedEntry();
        if (selected != null
                && localMouseX >= detailsPaneLeft + 4
                && localMouseX <= detailsPaneLeft + detailsPaneWidth - 4
                && localMouseY >= detailsTop
                && localMouseY <= detailsContentBottom) {
            detailsScroll = Mth.clamp(detailsScroll - ((int) Math.signum(scrollY) * 10), 0, maxDetailsScroll(selected));
            return true;
        }
        return super.mouseScrolled(localMouseX, localMouseY, scrollX, scrollY);
    }

    @Override
    public void render(@Nonnull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        if (!openSoundPlayed) {
            playLocalSound("item.book.page_turn", 0.9F, 1.15F);
            openSoundPlayed = true;
        }
        double localMouseX = toLocalX(mouseX);
        double localMouseY = toLocalY(mouseY);

        // 1. Render the blurred world background once, then draw our opaque panels on top.
        //    We must NOT call super.render() after our panels because Screen.render() calls
        //    renderBackground() internally, which would redraw the blur over everything.
        renderBackground(guiGraphics, mouseX, mouseY, partialTick);

        // 2. Draw opaque window chrome
        guiGraphics.fill(0, 0, width, height, 0x88000000);
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(renderLeft, renderTop, 0.0F);
        guiGraphics.pose().scale(uiScale, uiScale, 1.0F);
        drawFrame(guiGraphics, left, top, WINDOW_WIDTH, WINDOW_HEIGHT, OUTER, ACCENT);
        drawFrame(guiGraphics, listPaneLeft, listPaneTop, listPaneWidth, listPaneHeight, PANEL_DARK, PANEL_LIGHT);
        drawFrame(guiGraphics, detailsPaneLeft, detailsPaneTop, detailsPaneWidth, detailsPaneHeight, PANEL_DARK, PANEL_LIGHT);

        guiGraphics.fill(searchBox.getX() - 2, searchBox.getY() - 2, searchBox.getX() + searchBox.getWidth() + 2, searchBox.getY() + searchBox.getHeight() + 2, ACCENT);
        guiGraphics.fill(searchBox.getX() - 1, searchBox.getY() - 1, searchBox.getX() + searchBox.getWidth() + 1, searchBox.getY() + searchBox.getHeight() + 1, 0xFFF9F2DA);

        guiGraphics.drawString(font, title, left + 14, top + 8, PANEL_LIGHT, false);
        guiGraphics.drawString(font, Component.translatable("createpop.brewers_notebook.subtitle", allEntries.size()), left + 126, top + 8, 0xFFD7C089, false);

        Component results = Component.translatable("createpop.brewers_notebook.results", filteredEntries.size());
        guiGraphics.drawString(font, Component.translatable("createpop.brewers_notebook.search"), listLeft, searchBox.getY() - 10, INK, false);
        guiGraphics.drawString(font, Component.translatable("createpop.brewers_notebook.recipes"), listLeft, listTop - 12, INK, false);
        guiGraphics.drawString(font, results, listLeft + listWidth - font.width(results), listTop - 12, MUTED, false);

        renderEntryList(guiGraphics, (int) Math.round(localMouseX), (int) Math.round(localMouseY));
        renderDetails(guiGraphics, (int) Math.round(localMouseX), (int) Math.round(localMouseY));

        // 3. Render widgets (buttons, editboxes) on top of panels
        for (net.minecraft.client.gui.components.Renderable renderable : this.renderables) {
            renderable.render(guiGraphics, (int) Math.round(localMouseX), (int) Math.round(localMouseY), partialTick);
        }
        guiGraphics.pose().popPose();

        if (hoveredTitleLink) {
            guiGraphics.renderTooltip(font, Component.translatable("createpop.brewers_guide.link_hint.recipes"), mouseX, mouseY);
        }
    }

    private void renderEntryList(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (filteredEntries.isEmpty()) {
            drawWrappedMuted(guiGraphics, Component.translatable("createpop.brewers_notebook.no_results"), listLeft, listTop + 8, listWidth);
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
            boolean hovered = mouseX >= listLeft && mouseX <= listLeft + listWidth && mouseY >= rowTop && mouseY < rowTop + ENTRY_HEIGHT;
            int fill = selected ? SELECTION : hovered ? HOVER : 0x2255402B;
            guiGraphics.fill(listLeft, rowTop, listLeft + listWidth, rowTop + ENTRY_HEIGHT - 1, fill);
            guiGraphics.fill(listLeft, rowTop + ENTRY_HEIGHT - 1, listLeft + listWidth, rowTop + ENTRY_HEIGHT, 0x332D2118);

            int textColor = selected ? 0xFFFDF5D3 : entry.data().rgbColor();
            drawMarqueeReadableString(guiGraphics, entry.name(), listLeft + 5, rowTop + 3, listWidth - 8, textColor);
            guiGraphics.drawString(font,
                    Component.translatable("createpop.brewers_notebook.effects_count", entry.data().effects().size()),
                    listLeft + 5, rowTop + 12, MUTED, false);
        }
    }

    private void renderDetails(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        BrewersNotebookData.Entry selected = selectedEntry();
        hoveredTitleLink = false;
        titleLinkLeft = 0;
        titleLinkTop = 0;
        titleLinkRight = 0;
        titleLinkBottom = 0;
        if (selected == null) {
            guiGraphics.drawString(font, Component.translatable("createpop.brewers_notebook.notes"), detailsLeft, notesHeaderY, INK, false);
            guiGraphics.fill(noteBoxLeft, noteBoxTop, noteBoxLeft + noteBoxWidth, noteBoxTop + (NOTE_LINES_PER_PAGE * 18) + 8, 0x30FFFFFF);
            drawWrappedMuted(guiGraphics, Component.translatable("createpop.brewers_notebook.empty_selection"), detailsLeft, detailsTop + 12, detailsWidth);
            return;
        }

        guiGraphics.enableScissor(toScreenX(detailsPaneLeft + 3), toScreenY(detailsTop), toScreenX(detailsPaneLeft + detailsPaneWidth - 3), toScreenY(detailsContentBottom));

        int y = detailsTop - detailsScroll;
        titleLinkLeft = detailsLeft;
        titleLinkTop = y;
        y = drawWrappedShadowed(guiGraphics, Component.literal(selected.name()).withStyle(style -> style.withColor(selected.data().rgbColor())),
                detailsLeft, y, detailsWidth, selected.data().rgbColor());
        titleLinkRight = detailsLeft + Math.min(detailsWidth, font.width(selected.name()));
        titleLinkBottom = y;
        hoveredTitleLink = jeiRecipeLinksAvailable && isPointInside(mouseX, mouseY, titleLinkLeft, titleLinkTop, titleLinkRight, titleLinkBottom);
        if (hoveredTitleLink) {
            guiGraphics.fill(titleLinkLeft, titleLinkBottom, Math.min(detailsLeft + detailsWidth, titleLinkLeft + font.width(selected.name())), titleLinkBottom + 1, selected.data().rgbColor());
        }

        guiGraphics.drawString(font,
                Component.translatable("createpop.brewers_notebook.instability_line", String.format(Locale.ROOT, "%.2f", selected.data().instability())),
                detailsLeft, y + 4, 0xFFB97638, false);
        y += 18;

        guiGraphics.drawString(font, Component.translatable("createpop.brewers_notebook.effects"), detailsLeft, y + 2, INK, false);
        y += 14;
        if (selected.data().effects().isEmpty()) {
            guiGraphics.drawString(font, Component.translatable("createpop.soda.tooltip.no_effects"), detailsLeft + 6, y, MUTED, false);
        } else {
            for (var effect : selected.data().effects()) {
                y = drawWrappedMuted(guiGraphics, SodaTextHelper.formatEffect(effect).copy().withStyle(ChatFormatting.DARK_GRAY), detailsLeft + 4, y, detailsWidth - 8);
            }
        }

        guiGraphics.disableScissor();
        drawDetailsScrollbar(guiGraphics, maxDetailsScroll(selected));

        int dividerY = notesHeaderY - 8;
        guiGraphics.fill(detailsLeft, dividerY, detailsLeft + detailsWidth, dividerY + 1, 0x553D2A1C);

        Component pageIndicator = Component.translatable("createpop.brewers_notebook.page_indicator", notePage + 1, Math.max(1, maxNotePage() + 1));
        guiGraphics.drawString(font, Component.translatable("createpop.brewers_notebook.notes"), detailsLeft, notesHeaderY, INK, false);
        guiGraphics.drawString(font, pageIndicator, notePrevButtonX - font.width(pageIndicator) - 6, notesHeaderY, MUTED, false);

        int noteBoxBottom = noteBoxTop + (NOTE_LINES_PER_PAGE * 18) + 8;
        guiGraphics.fill(noteBoxLeft, noteBoxTop, noteBoxLeft + noteBoxWidth, noteBoxBottom, 0x30FFFFFF);
        for (int i = 0; i < NOTE_LINES_PER_PAGE; i++) {
            int lineY = noteEditorsTop + (i * 18) + 12;
            guiGraphics.fill(noteBoxLeft + 6, lineY, noteBoxLeft + noteBoxWidth - 6, lineY + 1, 0x553D2A1C);
        }
    }

    private int maxDetailsScroll(BrewersNotebookData.Entry entry) {
        if (entry == null) {
            return 0;
        }
        return Math.max(0, measureDetailsHeight(entry) - (detailsContentBottom - detailsTop));
    }

    private void scrollListBy(int delta) {
        int nextScroll = Mth.clamp(listScroll + delta, 0, maxListScroll());
        if (nextScroll == listScroll) {
            return;
        }
        listScroll = nextScroll;
        playLocalSound("item.book.page_turn", 0.75F, 1.0F);
        init();
    }

    private void moveListSelection(int delta) {
        if (filteredEntries.isEmpty()) {
            return;
        }
        int currentIndex = selectedFilteredIndex();
        if (currentIndex < 0) {
            currentIndex = listScroll;
        }
        int targetIndex = Mth.clamp(currentIndex + delta, 0, filteredEntries.size() - 1);
        if (targetIndex == currentIndex && listScroll == Mth.clamp(listScroll + delta, 0, maxListScroll())) {
            return;
        }
        syncVisibleNoteEditors();
        selectedKey = filteredEntries.get(targetIndex).key();
        editableNoteLines.clear();
        loadedNoteKey = null;
        notePage = 0;
        detailsScroll = 0;
        confirmingDelete = false;
        if (targetIndex < listScroll) {
            listScroll = targetIndex;
        } else if (targetIndex >= listScroll + VISIBLE_ENTRIES) {
            listScroll = targetIndex - VISIBLE_ENTRIES + 1;
        }
        playLocalSound("item.book.page_turn", 0.9F, 1.0F);
        init();
    }

    private boolean clickTitleRecipeLink(double mouseX, double mouseY) {
        BrewersNotebookData.Entry selected = selectedEntry();
        if (!jeiRecipeLinksAvailable || selected == null) {
            return false;
        }
        if (!isPointInside(mouseX, mouseY, titleLinkLeft, titleLinkTop, titleLinkRight, titleLinkBottom)) {
            return false;
        }
        if (minecraft != null) {
            minecraft.execute(() -> openSodaRecipeInJei(selected.data()));
            return true;
        }
        return false;
    }

    private int selectedFilteredIndex() {
        if (selectedKey == null) {
            return -1;
        }
        for (int i = 0; i < filteredEntries.size(); i++) {
            if (filteredEntries.get(i).key().equals(selectedKey)) {
                return i;
            }
        }
        return -1;
    }

    private int measureDetailsHeight(BrewersNotebookData.Entry entry) {
        int height = measureWrappedHeight(Component.literal(entry.name()), detailsWidth);
        height += 18;
        height += 14;
        if (entry.data().effects().isEmpty()) {
            height += 10;
        } else {
            for (var effect : entry.data().effects()) {
                height += measureWrappedHeight(SodaTextHelper.formatEffect(effect).copy().withStyle(ChatFormatting.DARK_GRAY), detailsWidth - 8);
            }
        }
        return height;
    }

    private int measureWrappedHeight(Component text, int maxWidth) {
        return font.split(text, maxWidth).size() * 10;
    }

    private int drawWrappedMuted(GuiGraphics guiGraphics, Component text, int x, int y, int maxWidth) {
        List<net.minecraft.util.FormattedCharSequence> lines = font.split(text, maxWidth);
        for (net.minecraft.util.FormattedCharSequence line : lines) {
            guiGraphics.drawString(font, line, x, y, MUTED, false);
            y += 10;
        }
        return y;
    }

    private int drawWrappedShadowed(GuiGraphics guiGraphics, Component text, int x, int y, int maxWidth, int color) {
        List<net.minecraft.util.FormattedCharSequence> lines = font.split(text, maxWidth);
        for (net.minecraft.util.FormattedCharSequence line : lines) {
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

    private void drawMarqueeReadableString(GuiGraphics guiGraphics, String text, int x, int y, int maxWidth, int color) {
        int textWidth = font.width(text);
        if (textWidth <= maxWidth) {
            drawReadableString(guiGraphics, text, x, y, color);
            return;
        }

        int gap = 12;
        int offset = marqueeOffset(textWidth, maxWidth, gap);
        guiGraphics.enableScissor(toScreenX(x), toScreenY(y), toScreenX(x + maxWidth), toScreenY(y + 10));
        drawReadableString(guiGraphics, text, x - offset, y, color);
        drawReadableString(guiGraphics, text, x + textWidth + gap - offset, y, color);
        guiGraphics.disableScissor();
    }

    private int marqueeOffset(int textWidth, int maxWidth, int gap) {
        int travel = textWidth - maxWidth;
        if (travel <= 0) {
            return 0;
        }
        int pause = 900;
        int pixelsPerSecond = 24;
        int loop = travel + gap;
        long travelTime = Math.round((loop * 1000.0F) / pixelsPerSecond);
        long cycleTime = (pause * 3L) + (travelTime * 2L);
        long phase = Util.getMillis() % cycleTime;
        if (phase < pause) {
            return 0;
        }
        phase -= pause;
        if (phase < travelTime) {
            return Math.min(loop, (int) Math.round((phase / (double) travelTime) * loop));
        }
        phase -= travelTime;
        if (phase < pause) {
            return loop;
        }
        phase -= pause;
        if (phase < travelTime) {
            return Math.max(0, loop - (int) Math.round((phase / (double) travelTime) * loop));
        }
        return 0;
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

    @Override
    public void onClose() {
        playLocalSound("item.book.page_turn", 0.8F, 0.85F);
        super.onClose();
    }

    private boolean queryJeiRecipeLinksAvailable() {
        try {
            return CreatePopJeiPlugin.isRuntimeAvailable();
        } catch (Throwable ignored) {
            return false;
        }
    }

    private void openSodaRecipeInJei(org.neonalig.createpop.component.SodaData data) {
        try {
            CreatePopJeiPlugin.showSodaRecipe(data);
        } catch (Throwable ignored) {
        }
    }

    private void playLocalSound(String soundId, float volume, float pitch) {
        if (minecraft == null || minecraft.player == null) {
            return;
        }
        minecraft.player.playSound(SoundEvent.createVariableRangeEvent(ResourceLocation.withDefaultNamespace(soundId)), volume, pitch);
    }

    private double toLocalX(double screenX) {
        return (screenX - renderLeft) / uiScale;
    }

    private double toLocalY(double screenY) {
        return (screenY - renderTop) / uiScale;
    }

    private int toScreenX(int localX) {
        return renderLeft + Math.round(localX * uiScale);
    }

    private int toScreenY(int localY) {
        return renderTop + Math.round(localY * uiScale);
    }

    private boolean isPointInside(double mouseX, double mouseY, int minX, int minY, int maxX, int maxY) {
        return mouseX >= minX && mouseX <= maxX && mouseY >= minY && mouseY <= maxY;
    }

    private boolean hasPendingChanges() {
        BrewersNotebookData.Entry selected = selectedEntry();
        if (selected == null) {
            return false;
        }
        return !currentEditedNote().equals(selected.note());
    }

    private String currentEditedNote() {
        List<String> noteLines = new ArrayList<>(editableNoteLines);
        if (!noteEditors.isEmpty()) {
            ensureEditableCapacity(noteLines, (notePage + 1) * NOTE_LINES_PER_PAGE);
            for (int line = 0; line < noteEditors.size(); line++) {
                noteLines.set((notePage * NOTE_LINES_PER_PAGE) + line, noteEditors.get(line).getValue());
            }
        }
        return joinedNote(noteLines);
    }

    private void ensureEditableCapacity(List<String> noteLines, int size) {
        while (noteLines.size() < size) {
            noteLines.add("");
        }
    }

    private String joinedNote(List<String> noteLines) {
        int last = noteLines.size() - 1;
        while (last >= 0 && noteLines.get(last).isBlank()) {
            last--;
        }
        if (last < 0) {
            return "";
        }
        return String.join("\n", noteLines.subList(0, last + 1));
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


