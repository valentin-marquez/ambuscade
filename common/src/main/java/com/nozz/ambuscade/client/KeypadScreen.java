package com.nozz.ambuscade.client;

import static org.lwjgl.glfw.GLFW.GLFW_KEY_BACKSPACE;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_KP_0;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_KP_9;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_KP_ENTER;

import com.nozz.ambuscade.block.Keypad;
import dev.architectury.networking.NetworkManager;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** A steel keypad: twelve keys and a readout. Setting a code shows the digits, entering one shows dots. */
public final class KeypadScreen extends Screen {
    private static final int W = 148, H = 226, KEY = 28, GAP = 6;
    private static final int FRAME = 0xFF1D2030, PLATE = 0xFF4D5369, PLATE_LIT = 0xFF8A90AE, PLATE_DARK = 0xFF31374A;
    private static final int SCREEN = 0xFF10261C, SCREEN_INK = 0xFF5FD38C, LABEL = 0xFFBCC0D6;

    private final Keypad.Open data;
    private String code = "";
    private int echo = -1;   // the digit a numpad key already typed: with Num Lock on, it also arrives as a character

    public KeypadScreen(Keypad.Open data) {
        super(Component.translatable("item.ambuscade.keypad_lock"));
        this.data = data;
    }

    private int left() {
        return (width - W) / 2;
    }

    private int top() {
        return (height - H) / 2;
    }

    @Override
    protected void init() {
        int x = left() + (W - 3 * KEY - 2 * GAP) / 2, y = top() + 66;
        String[] keys = {"1", "2", "3", "4", "5", "6", "7", "8", "9", "<", "0", "OK"};
        for (int i = 0; i < keys.length; i++) {
            String k = keys[i];
            addRenderableWidget(Button.builder(Component.literal(k), b -> press(k))
                    .bounds(x + i % 3 * (KEY + GAP), y + i / 3 * (KEY + GAP), KEY, KEY).build());
        }
    }

    private void press(String key) {
        switch (key) {
            case "<" -> code = code.isEmpty() ? code : code.substring(0, code.length() - 1);
            case "OK" -> {
                if (!Keypad.valid(code)) return;
                NetworkManager.sendToServer(new Keypad.Submit(data.door(), data.setting(), code));
                onClose();
            }
            default -> {
                if (code.length() < Keypad.MAX_DIGITS) code += key;
            }
        }
    }

    @Override
    public boolean charTyped(char c, int modifiers) {
        if (c >= '0' && c <= '9') {
            if (c != echo) press(Character.toString(c));
            echo = -1;
            return true;
        }
        return super.charTyped(c, modifiers);
    }

    @Override
    public boolean keyPressed(int key, int scanCode, int modifiers) {
        echo = -1;
        if (key >= GLFW_KEY_KP_0 && key <= GLFW_KEY_KP_9) {   // the numpad, with or without Num Lock
            echo = '0' + key - GLFW_KEY_KP_0;
            press(Character.toString(echo));
            return true;
        }
        switch (key) {
            case GLFW_KEY_BACKSPACE -> press("<");
            case GLFW_KEY_ENTER, GLFW_KEY_KP_ENTER -> press("OK");
            default -> {
                return super.keyPressed(key, scanCode, modifiers);
            }
        }
        return true;
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(g, mouseX, mouseY, partialTick);
        int x = left(), y = top();
        g.fill(x - 3, y - 3, x + W + 3, y + H + 3, FRAME);
        g.fill(x - 2, y - 2, x + W + 2, y + H + 2, PLATE_LIT);
        g.fill(x - 1, y - 1, x + W + 2, y + H + 2, PLATE_DARK);
        g.fill(x - 1, y - 1, x + W + 1, y + H + 1, PLATE);
        for (int[] r : new int[][] {{4, 4}, {W - 6, 4}, {4, H - 6}, {W - 6, H - 6}}) {
            g.fill(x + r[0], y + r[1], x + r[0] + 2, y + r[1] + 2, PLATE_LIT);
            g.fill(x + r[0] + 1, y + r[1] + 1, x + r[0] + 2, y + r[1] + 2, FRAME);
        }
        g.fill(x + 24, y + 30, x + W - 24, y + 52, FRAME);
        g.fill(x + 25, y + 31, x + W - 25, y + 51, SCREEN);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        int x = left(), y = top();
        Component label = Component.translatable(data.setting() ? "gui.ambuscade.keypad.set" : "gui.ambuscade.keypad.enter");
        g.drawString(font, label, x + (W - font.width(label)) / 2, y + 14, LABEL, false);
        String shown = data.setting() ? code : "*".repeat(code.length());
        if (shown.isEmpty()) shown = "_";
        g.drawString(font, shown, x + (W - font.width(shown)) / 2, y + 37, SCREEN_INK, false);
        Component hint = Component.translatable("gui.ambuscade.keypad.hint", Keypad.MIN_DIGITS, Keypad.MAX_DIGITS);
        g.drawString(font, hint, x + (W - font.width(hint)) / 2, y + H - 18, LABEL, false);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
