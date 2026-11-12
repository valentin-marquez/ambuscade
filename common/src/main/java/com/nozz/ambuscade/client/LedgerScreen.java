package com.nozz.ambuscade.client;

import com.nozz.ambuscade.AmbuscadeRegistry;
import com.nozz.ambuscade.ledger.Ledger;
import com.nozz.ambuscade.ledger.Ledger.Creature;
import com.nozz.ambuscade.ledger.LedgerNetwork;
import dev.architectury.networking.NetworkManager;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.core.UUIDUtil;
import net.minecraft.world.level.block.entity.SkullBlockEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;

/** Ledger-styled panel drawn with fills, every control a vanilla widget. Two tabs: the list and the linked traps. */
public final class LedgerScreen extends Screen {
    private static final int GLFW_KEY_TAB = 258;
    private static final int W = 232, H = 316, ROWS = 3, ROW_H = 22, TAB_H = 20;
    private static final int LEATHER = 0xFF6B4A2F, LEATHER_DARK = 0xFF3B2A1B, PAGE = 0xFFEBDFC4, PAGE_SHADE = 0xFFD9CBAA;
    private static final int INK = 0xFF3B2A1B, INK_SOFT = 0xFF6B5A44, LIST = 0xFF1E1E1E, LIST_EDGE = 0xFF5A5246;
    private static final int WHITE = 0xFFFFFFFF, GRAY = 0xFF9A9A9A;
    private static final ItemStack[] CREATURE_ICONS = {
            new ItemStack(Items.BONE), new ItemStack(Items.WHEAT), new ItemStack(Items.EMERALD), new ItemStack(Items.ROTTEN_FLESH)};

    private final UUID id;
    private boolean target;
    private List<String> names;
    private int creatures;
    private List<LedgerNetwork.Trap> traps;
    private int tab;
    private int scroll, trapScroll;
    private String typed = "";
    private EditBox nameBox;
    private final List<Button> creatureButtons = new ArrayList<>();
    private final List<String> suggestions = new ArrayList<>();
    private int h = H;

    public LedgerScreen(LedgerNetwork.Open data) {
        super(Component.translatable("item.ambuscade.perimeter_ledger"));
        this.id = data.id();
        apply(data);
    }

    public UUID id() {
        return id;
    }

    public void update(LedgerNetwork.Open data) {
        if (nameBox != null) typed = nameBox.getValue();
        if (data.names().contains(typed.toLowerCase(Locale.ROOT))) typed = "";
        apply(data);
        rebuildWidgets();
    }

    private void apply(LedgerNetwork.Open data) {
        target = data.target();
        names = data.names();
        creatures = data.creatures();
        traps = new ArrayList<>(data.traps());
        traps.sort(Comparator.comparingDouble(this::distance));
        scroll = clamp(scroll, names.size() - ROWS);
        trapScroll = clamp(trapScroll, traps.size() - trapRows());
    }

    private static int clamp(int value, int max) {
        return Math.max(0, Math.min(value, max));
    }

    private int left() {
        return (width - W) / 2;
    }

    private int top() {
        return Math.max(TAB_H + 6, (height - h + TAB_H) / 2);
    }

    private int trapRows() {
        return (h - 34 - 44 - 4) / ROW_H;
    }

    @Override
    protected void init() {
        suggestions.clear();
        if (minecraft.getConnection() != null) {
            for (PlayerInfo info : minecraft.getConnection().getOnlinePlayers()) {
                String n = info.getProfile().getName();
                if (suggestions.size() < 2 && !n.equals(minecraft.getUser().getName()) && !names.contains(n.toLowerCase(Locale.ROOT))) suggestions.add(n);
            }
        }
        h = suggestions.isEmpty() ? H - 36 : H;   // no one online to suggest: drop that row
        int x = left(), y = top();

        int tabW = (W + 6) / 2;   // the two tabs together cover the book, resting on its leather edge
        Button listTab = Button.builder(tabLabel(0), b -> switchTab(0)).bounds(x - 3, y - 3 - TAB_H, tabW, TAB_H).build();
        Button trapTab = Button.builder(tabLabel(1), b -> switchTab(1)).bounds(x - 3 + tabW, y - 3 - TAB_H, W + 6 - tabW, TAB_H).build();
        addRenderableWidget(listTab);
        addRenderableWidget(trapTab);

        if (tab == 0) initList(x, y);
        else initTraps(x, y);
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose()).bounds(x + W / 2 - 50, y + h - 26, 100, 20).build());
    }

    private void switchTab(int next) {
        if (next == tab) return;
        if (nameBox != null) typed = nameBox.getValue();
        tab = next;
        rebuildWidgets();
    }

    private Component tabLabel(int index) {
        Component label = Component.translatable(index == 0 ? "gui.ambuscade.ledger.tab.list" : "gui.ambuscade.ledger.tab.traps");
        if (index == 1) label = label.copy().append(" (" + traps.size() + ")");
        return index == tab ? label.copy().withStyle(ChatFormatting.YELLOW) : label;
    }

    private void initList(int x, int y) {
        int inner = x + 14;
        Button protect = Button.builder(modeLabel(false), b -> { if (target) send(2, ""); }).bounds(inner + 40, y + 36, 82, 20).build();
        Button targets = Button.builder(modeLabel(true), b -> { if (!target) send(2, ""); }).bounds(inner + 124, y + 36, 82, 20).build();
        addRenderableWidget(protect);
        addRenderableWidget(targets);

        creatureButtons.clear();
        for (Creature c : Creature.values()) {
            int col = c.ordinal() % 2, row = c.ordinal() / 2;
            creatureButtons.add(addRenderableWidget(Button.builder(creatureLabel(c), b -> send(3, c.name()))
                    .bounds(inner + col * 104, y + 96 + row * 22, 100, 20).build()));
        }

        int listTop = y + 156;
        for (int i = scroll; i < Math.min(names.size(), scroll + ROWS); i++) {
            String name = names.get(i);
            addRenderableWidget(Button.builder(Component.literal("✕"), b -> send(1, name))
                    .bounds(inner + 182, listTop + 2 + (i - scroll) * ROW_H, 20, 20).build());
        }

        nameBox = new EditBox(font, inner, y + 228, 146, 20, Component.translatable("gui.ambuscade.ledger.name"));
        nameBox.setMaxLength(16);
        nameBox.setHint(Component.translatable("gui.ambuscade.ledger.name"));
        nameBox.setValue(typed);
        nameBox.setResponder(text -> nameBox.setSuggestion(completion(text) == null ? null : completion(text).substring(text.length())));
        addRenderableWidget(nameBox);
        addRenderableWidget(Button.builder(Component.translatable("gui.ambuscade.ledger.add"), b -> {
            if (Ledger.validName(nameBox.getValue())) send(0, nameBox.getValue());
        }).bounds(inner + 150, y + 228, 54, 20).build());

        for (int i = 0; i < suggestions.size(); i++) {
            String n = suggestions.get(i);
            addRenderableWidget(Button.builder(Component.literal("    " + n + " +"), b -> send(0, n))
                    .bounds(inner + i * 104, y + 266, 100, 20).build());
        }
    }

    private void initTraps(int x, int y) {
        nameBox = null;
        int inner = x + 14, listTop = y + 44;
        for (int i = trapScroll; i < Math.min(traps.size(), trapScroll + trapRows()); i++) {
            LedgerNetwork.Trap trap = traps.get(i);
            addRenderableWidget(Button.builder(Component.literal("✕"), b -> NetworkManager.sendToServer(new LedgerNetwork.Edit(id, 4, "", trap.pos())))
                    .bounds(inner + 182, listTop + 2 + (i - trapScroll) * ROW_H, 20, 20).build());
        }
    }

    private Component modeLabel(boolean targetMode) {
        Component label = Component.translatable(targetMode ? "gui.ambuscade.ledger.mode.target" : "gui.ambuscade.ledger.mode.protect");
        return targetMode == target ? label.copy().withStyle(ChatFormatting.YELLOW) : label;
    }

    private boolean has(Creature c) {
        return (creatures & (1 << c.ordinal())) != 0;
    }

    private Component creatureLabel(Creature c) {
        Component label = Component.literal("   ").append(Component.translatable("gui.ambuscade.ledger.creature." + c.name().toLowerCase(Locale.ROOT)));
        return has(c) ? label.copy().withStyle(ChatFormatting.YELLOW) : label;
    }

    private void send(int action, String name) {
        NetworkManager.sendToServer(new LedgerNetwork.Edit(id, action, name, LedgerNetwork.Edit.NOWHERE));
    }

    private double distance(LedgerNetwork.Trap trap) {
        if (minecraft == null || minecraft.player == null || minecraft.level == null || !trap.pos().dimension().equals(minecraft.level.dimension())) {
            return Double.MAX_VALUE;
        }
        return Math.sqrt(minecraft.player.position().distanceToSqr(Vec3.atCenterOf(trap.pos().pos())));
    }

    // the ledger stores names lower-cased; the tab list keeps the real casing
    private PlayerInfo onlineInfo(String name) {
        if (minecraft.getConnection() == null) return null;
        return minecraft.getConnection().getOnlinePlayers().stream()
                .filter(i -> i.getProfile().getName().equalsIgnoreCase(name)).findFirst().orElse(null);
    }

    /** First online player whose name starts with what was typed, like chat tab-completion. */
    private String completion(String typed) {
        if (typed.isEmpty() || minecraft.getConnection() == null) return null;
        String lower = typed.toLowerCase(Locale.ROOT);
        return minecraft.getConnection().getOnlinePlayers().stream().map(i -> i.getProfile().getName())
                .filter(n -> n.toLowerCase(Locale.ROOT).startsWith(lower) && n.length() > typed.length())
                .sorted().findFirst().orElse(null);
    }

    @Override
    public boolean keyPressed(int key, int scanCode, int modifiers) {
        if (key == GLFW_KEY_TAB && nameBox != null && nameBox.isFocused()) {
            String full = completion(nameBox.getValue());
            if (full != null) {
                nameBox.setValue(full);
                nameBox.moveCursorToEnd(false);
            }
            return true;
        }
        return super.keyPressed(key, scanCode, modifiers);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int step = -(int) Math.signum(scrollY);
        if (tab == 0) {
            int next = clamp(scroll + step, names.size() - ROWS);
            if (next == scroll) return true;
            typed = nameBox.getValue();
            scroll = next;
        } else {
            int next = clamp(trapScroll + step, traps.size() - trapRows());
            if (next == trapScroll) return true;
            trapScroll = next;
        }
        rebuildWidgets();
        return true;
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(g, mouseX, mouseY, partialTick);
        int x = left(), y = top();
        g.fill(x - 3, y - 3, x + W + 3, y + h + 3, LEATHER_DARK);
        g.fill(x - 2, y - 2, x + W + 2, y + h + 2, LEATHER);
        g.fill(x + 4, y + 4, x + W - 4, y + h - 4, PAGE_SHADE);
        g.fill(x + 4, y + 4, x + W - 5, y + h - 5, PAGE);
        for (int sy = y + 10; sy < y + h - 10; sy += 6) g.fill(x + 7, sy, x + 8, sy + 3, LEATHER);   // stitched spine
        int listTop = tab == 0 ? y + 156 : y + 44;
        int listBottom = tab == 0 ? listTop + 2 + ROWS * ROW_H + 2 : listTop + 2 + trapRows() * ROW_H + 2;
        g.fill(x + 13, listTop - 1, x + W - 13, listBottom + 1, LIST_EDGE);
        g.fill(x + 14, listTop, x + W - 14, listBottom, LIST);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        int x = left(), y = top(), inner = x + 14;

        g.renderItem(new ItemStack(AmbuscadeRegistry.PERIMETER_LEDGER.get()), inner, y + 10);
        g.drawString(font, title, inner + 22, y + 14, INK, false);

        if (tab == 0) renderList(g, x, y, inner);
        else renderTraps(g, x, y, inner);
    }

    private void renderList(GuiGraphics g, int x, int y, int inner) {
        g.drawString(font, Component.translatable("gui.ambuscade.ledger.mode"), inner, y + 42, INK, false);

        List<FormattedCharSequence> hint = font.split(Component.translatable(target ? "gui.ambuscade.ledger.hint.target" : "gui.ambuscade.ledger.hint.protect"), W - 28);
        for (int i = 0; i < Math.min(2, hint.size()); i++) {
            g.drawString(font, hint.get(i), x + (W - font.width(hint.get(i))) / 2, y + 62 + i * 10, INK_SOFT, false);
        }

        g.drawString(font, Component.translatable("gui.ambuscade.ledger.creatures"), inner, y + 86, INK, false);
        for (int i = 0; i < creatureButtons.size(); i++) {
            Button b = creatureButtons.get(i);
            g.renderItem(CREATURE_ICONS[i], b.getX() + 3, b.getY() + 2);
        }

        g.drawString(font, Component.translatable(target ? "gui.ambuscade.ledger.players.target" : "gui.ambuscade.ledger.players.protect"), inner, y + 146, INK, false);
        String count = names.size() + " / " + Ledger.MAX_NAMES;
        g.drawString(font, count, x + W - 14 - font.width(count), y + 146, INK_SOFT, false);

        int listTop = y + 156;
        for (int i = scroll; i < Math.min(names.size(), scroll + ROWS); i++) {
            String name = names.get(i);
            int ry = listTop + 2 + (i - scroll) * ROW_H;
            PlayerInfo info = onlineInfo(name);
            boolean online = info != null;
            PlayerFaceRenderer.draw(g, skinOf(name, info), inner + 4, ry + 4, 12);
            Component label = name.equalsIgnoreCase(minecraft.getUser().getName())
                    ? Component.literal(name + " ").append(Component.translatable("gui.ambuscade.ledger.you").withStyle(ChatFormatting.YELLOW))
                    : Component.literal(name);
            g.drawString(font, label, inner + 22, ry + 6, online ? WHITE : GRAY, true);
        }
        if (names.isEmpty()) g.drawString(font, Component.translatable("gui.ambuscade.ledger.empty"), inner + 6, listTop + 8, GRAY, false);
        scrollbar(g, x, listTop, ROWS, scroll, names.size());

        if (!suggestions.isEmpty()) {
            g.drawString(font, Component.translatable("gui.ambuscade.ledger.online"), inner, y + 254, INK_SOFT, false);
            for (int i = 0; i < suggestions.size(); i++) {
                String n = suggestions.get(i);
                PlayerFaceRenderer.draw(g, skinOf(n, minecraft.getConnection().getPlayerInfo(n)), inner + i * 104 + 6, y + 270, 12);
            }
        }
    }

    private void renderTraps(GuiGraphics g, int x, int y, int inner) {
        g.drawString(font, Component.translatable("gui.ambuscade.ledger.traps"), inner, y + 32, INK, false);
        String count = traps.size() + " / " + Ledger.MAX_TRAPS;
        g.drawString(font, count, x + W - 14 - font.width(count), y + 32, INK_SOFT, false);

        int listTop = y + 44, rows = trapRows();
        for (int i = trapScroll; i < Math.min(traps.size(), trapScroll + rows); i++) {
            LedgerNetwork.Trap trap = traps.get(i);
            int ry = listTop + 2 + (i - trapScroll) * ROW_H;
            Block block = BuiltInRegistries.BLOCK.get(ResourceLocation.parse(trap.block()));
            g.renderItem(new ItemStack(block.asItem()), inner + 3, ry + 3);
            g.drawString(font, block.getName(), inner + 22, ry + 2, WHITE, true);
            var p = trap.pos().pos();
            g.drawString(font, p.getX() + ", " + p.getY() + ", " + p.getZ(), inner + 22, ry + 12, GRAY, false);
            double d = distance(trap);
            Component where = d == Double.MAX_VALUE
                    ? Component.literal(trap.pos().dimension().location().getPath())
                    : Component.translatable("gui.ambuscade.ledger.meters", (int) Math.round(d));
            g.drawString(font, where, inner + 178 - font.width(where), ry + 7, d == Double.MAX_VALUE ? GRAY : WHITE, true);
        }
        if (traps.isEmpty()) {
            List<FormattedCharSequence> help = font.split(Component.translatable("gui.ambuscade.ledger.no_traps"), W - 40);
            for (int i = 0; i < help.size(); i++) g.drawString(font, help.get(i), inner + 6, listTop + 8 + i * 10, GRAY, false);
        }
        scrollbar(g, x, listTop, rows, trapScroll, traps.size());
    }

    private void scrollbar(GuiGraphics g, int x, int listTop, int rows, int offset, int total) {
        if (total <= rows) return;
        int track = rows * ROW_H, thumb = Math.max(8, track * rows / total);
        int ty = listTop + 2 + (track - thumb) * offset / Math.max(1, total - rows);
        g.fill(x + W - 18, ty, x + W - 15, ty + thumb, 0xFFA0A0A0);
    }

    // online players carry their skin; offline ones resolve by name through the same cache player heads use
    private PlayerSkin skinOf(String name, PlayerInfo info) {
        if (info != null) return info.getSkin();
        return SkullBlockEntity.fetchGameProfile(name).getNow(Optional.empty()).map(minecraft.getSkinManager()::getInsecureSkin)
                .orElseGet(() -> DefaultPlayerSkin.get(UUIDUtil.createOfflinePlayerUUID(name)));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
