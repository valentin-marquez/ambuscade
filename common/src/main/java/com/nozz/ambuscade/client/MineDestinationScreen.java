package com.nozz.ambuscade.client;

import com.nozz.ambuscade.AmbuscadeRegistry;
import com.nozz.ambuscade.block.MineNetwork;
import dev.architectury.networking.NetworkManager;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/** Where a teleport mine sends its victim: three coordinate boxes on the same leather-and-paper panel as the ledger. */
public final class MineDestinationScreen extends Screen {
    private static final int W = 220, H = 150;
    private static final int LEATHER = 0xFF6B4A2F, LEATHER_DARK = 0xFF3B2A1B, PAGE = 0xFFEBDFC4, PAGE_SHADE = 0xFFD9CBAA;
    private static final int INK = 0xFF3B2A1B, INK_SOFT = 0xFF6B5A44, ERROR = 0xFFB02E26;

    private final BlockPos mine;
    private BlockPos destination;
    private final EditBox[] boxes = new EditBox[3];
    private boolean invalid;

    public MineDestinationScreen(MineNetwork.OpenDestination data) {
        super(Component.translatable("block.ambuscade.teleport_mine"));
        this.mine = data.mine();
        this.destination = data.destination();
    }

    private int left() {
        return (width - W) / 2;
    }

    private int top() {
        return (height - H) / 2;
    }

    @Override
    protected void init() {
        int x = left(), y = top(), inner = x + 14;
        int[] values = {destination.getX(), destination.getY(), destination.getZ()};
        for (int i = 0; i < 3; i++) {
            boxes[i] = new EditBox(font, inner + 12 + i * 66, y + 52, 50, 20, Component.literal("xyz".substring(i, i + 1)));
            boxes[i].setMaxLength(9);
            boxes[i].setValue(Integer.toString(values[i]));
            addRenderableWidget(boxes[i]);
        }
        addRenderableWidget(Button.builder(Component.translatable("gui.ambuscade.mine.here"), b -> {
            BlockPos here = minecraft.player.blockPosition();
            boxes[0].setValue(Integer.toString(here.getX()));
            boxes[1].setValue(Integer.toString(here.getY()));
            boxes[2].setValue(Integer.toString(here.getZ()));
        }).bounds(inner, y + 92, W - 28, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> confirm()).bounds(x + W / 2 - 50, y + H - 30, 100, 20).build());
    }

    private void confirm() {
        try {
            BlockPos pos = new BlockPos(Integer.parseInt(boxes[0].getValue()), Integer.parseInt(boxes[1].getValue()), Integer.parseInt(boxes[2].getValue()));
            double dx = pos.getX() - mine.getX(), dz = pos.getZ() - mine.getZ();
            if (dx * dx + dz * dz > (double) MineNetwork.MAX_DISTANCE * MineNetwork.MAX_DISTANCE) {
                invalid = true;
                return;
            }
            NetworkManager.sendToServer(new MineNetwork.SetDestination(mine, pos));
            onClose();
        } catch (NumberFormatException e) {
            invalid = true;
        }
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(g, mouseX, mouseY, partialTick);
        int x = left(), y = top();
        g.fill(x - 3, y - 3, x + W + 3, y + H + 3, LEATHER_DARK);
        g.fill(x - 2, y - 2, x + W + 2, y + H + 2, LEATHER);
        g.fill(x + 4, y + 4, x + W - 4, y + H - 4, PAGE_SHADE);
        g.fill(x + 4, y + 4, x + W - 5, y + H - 5, PAGE);
        for (int sy = y + 10; sy < y + H - 10; sy += 6) g.fill(x + 7, sy, x + 8, sy + 3, LEATHER);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        int x = left(), y = top(), inner = x + 14;
        g.renderItem(new ItemStack(AmbuscadeRegistry.TELEPORT_MINE.get()), inner, y + 10);
        g.drawString(font, title, inner + 22, y + 14, INK, false);
        g.drawString(font, Component.translatable("gui.ambuscade.mine.destination"), inner, y + 38, INK, false);
        for (int i = 0; i < 3; i++) g.drawString(font, "XYZ".substring(i, i + 1), inner + 2 + i * 66, y + 58, INK, false);
        Component hint = invalid ? Component.translatable("gui.ambuscade.mine.invalid", MineNetwork.MAX_DISTANCE)
                : Component.translatable("gui.ambuscade.mine.hint", MineNetwork.MAX_DISTANCE);
        g.drawString(font, hint, x + (W - font.width(hint)) / 2, y + 78, invalid ? ERROR : INK_SOFT, false);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
