package art.arcane.bibliocraft.client;

import art.arcane.bibliocraft.BiblioCraft;
import art.arcane.bibliocraft.blockentity.ClockSettings;
import art.arcane.bibliocraft.menu.ClockMenu;
import art.arcane.bibliocraft.network.ModNetwork;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import java.util.Locale;

final class ClockScreen extends AbstractContainerScreen<ClockMenu> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(BiblioCraft.MODID, "textures/gui/clockgui.png");
    private static final int[] PRESET_INTERVALS = {1, 2, 4, 12, 0};
    private final boolean[] toggles = new boolean[4];
    private final long[] schedules = new long[2];

    ClockScreen(ClockMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        ClockSettings settings = menu.openedSettings();
        schedules[0] = settings.chimes();
        schedules[1] = settings.redstone();
        toggles[0] = settings.ticking();
        toggles[1] = settings.chimeEnabled();
        toggles[2] = settings.redstoneEnabled();
        toggles[3] = settings.pulse();
    }

    @Override
    protected void init() {
        super.init();
        for (int ring = 0; ring < schedules.length; ring++) {
            int schedule = ring;
            int x = ring == 0 ? 3 : 157;
            for (int preset = 0; preset < PRESET_INTERVALS.length; preset++) {
                int interval = PRESET_INTERVALS[preset];
                String label = interval == 0 ? "C" : Integer.toString(preset + 1);
                Component tooltip = interval == 0 ? text("clear")
                        : text("preset", interval * 30);
                addRenderableWidget(Button.builder(Component.literal(label),
                                button -> schedules[schedule] = preset(interval))
                        .bounds(leftPos + x, topPos + 20 + preset * 22, 16, 20)
                        .tooltip(Tooltip.create(tooltip)).build());
            }
        }
        addToggle(0, 5, 147);
        addToggle(1, 65, 147);
        addToggle(2, 125, 147);
        addToggle(3, 125, 131);
    }

    private static long preset(int interval) {
        long result = 0;
        if (interval > 0) {
            for (int slot = 0; slot < ClockSettings.SLOTS; slot += interval) {
                result |= 1L << slot;
            }
        }
        return result;
    }

    private void addToggle(int index, int x, int y) {
        addRenderableWidget(Button.builder(toggleTitle(index), button -> {
            toggles[index] = !toggles[index];
            button.setMessage(toggleTitle(index));
        }).bounds(leftPos + x, topPos + y, 46, 14).tooltip(Tooltip.create(text("toggle_" + index))).build());
    }

    private Component toggleTitle(int index) {
        if (index == 3) {
            return text(toggles[index] ? "pulse" : "solid");
        }
        return text(switch (index) {
            case 0 -> "tick";
            case 1 -> "chime";
            default -> "power";
        }).copy().withStyle(toggles[index] ? ChatFormatting.GREEN : ChatFormatting.GRAY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        for (int ring = 0; ring < schedules.length; ring++) {
            for (int slot = 0; slot < ClockSettings.SLOTS; slot++) {
                int u = ClockSettings.includes(schedules[ring], slot) ? 7 * (ring + 1) : 0;
                graphics.blit(TEXTURE, markerX(ring, slot), markerY(ring, slot), u, 170, 5, 5);
            }
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        drawPresetHeading(graphics, text("chime"), 11, 0xff227a22);
        drawPresetHeading(graphics, text("power"), 165, 0xff941f18);
    }

    private void drawPresetHeading(GuiGraphics graphics, Component label, int center, int color) {
        int width = font.width(label);
        float scale = Math.min(0.62F, 20.0F / Math.max(1, width));
        graphics.pose().pushPose();
        graphics.pose().translate(center, 8, 0);
        graphics.pose().scale(scale, scale, 1);
        graphics.drawString(font, label, -width / 2, 0, color, false);
        graphics.pose().popPose();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        for (int ring = 0; ring < schedules.length; ring++) {
            int slot = markerAt(ring, mouseX, mouseY);
            if (slot >= 0) {
                String time = String.format(Locale.ROOT, "%02d:%02d", slot / 2, (slot % 2) * 30);
                graphics.renderTooltip(font, text(ring == 0 ? "chime_at" : "power_at", time), mouseX, mouseY);
                break;
            }
        }
    }

    @Override
    public boolean mouseClicked(double x, double y, int button) {
        if (button == 0) {
            for (int ring = 0; ring < schedules.length; ring++) {
                int slot = markerAt(ring, x, y);
                if (slot >= 0) {
                    schedules[ring] ^= 1L << slot;
                    return true;
                }
            }
        }
        return super.mouseClicked(x, y, button);
    }

    private int markerAt(int ring, double x, double y) {
        for (int slot = 0; slot < ClockSettings.SLOTS; slot++) {
            int left = markerX(ring, slot);
            int top = markerY(ring, slot);
            if (x >= left && x < left + 5 && y >= top && y < top + 5) {
                return slot;
            }
        }
        return -1;
    }

    private int markerX(int ring, int slot) {
        return leftPos + 86 + (int) ((ring == 0 ? 64 : 56) * Math.cos(angle(slot)));
    }

    private int markerY(int ring, int slot) {
        return topPos + 68 + (int) ((ring == 0 ? 64 : 57) * Math.sin(angle(slot)));
    }

    private static double angle(int slot) {
        return Math.toRadians(90 + slot * 360.0 / ClockSettings.SLOTS);
    }

    @Override
    public void onClose() {
        ModNetwork.CHANNEL.sendToServer(new ModNetwork.ClockPacket(menu.containerId,
                new ClockSettings(schedules[0], schedules[1], toggles[0], toggles[1], toggles[2], toggles[3])));
        super.onClose();
    }

    private static Component text(String key, Object... args) {
        return Component.translatable("screen.bibliocraft.clock." + key, args);
    }
}
