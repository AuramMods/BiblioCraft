package art.arcane.bibliocraft.client;

import art.arcane.bibliocraft.block.FurnitureReading;
import art.arcane.bibliocraft.enchantment.ReadingEnchantment;
import art.arcane.bibliocraft.network.ModNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class ReadingOverlay {
    private static final int REFRESH_TICKS = 4;
    private static ClientLevel level;
    private static FurnitureReading.Target target;
    private static List<Component> text = List.of();
    private static int cooldown;
    private static int sequence;
    private static int lastResponse;

    private ReadingOverlay() {}

    public static void tick() {
        var minecraft = Minecraft.getInstance();
        if (level != minecraft.level) {
            level = minecraft.level;
            target = null;
            text = List.of();
            cooldown = 0;
            lastResponse = sequence;
        }
        var current = target(minecraft);
        if (current == null || !current.equals(target)) {
            target = current;
            text = List.of();
            lastResponse = sequence;
        }
        if (cooldown > 0) {
            cooldown--;
        }
        if (target != null && cooldown == 0) {
            cooldown = REFRESH_TICKS;
            ModNetwork.CHANNEL.sendToServer(new ModNetwork.ReadingRequest(++sequence));
        }
    }

    public static void receive(ModNetwork.ReadingResponse response) {
        var minecraft = Minecraft.getInstance();
        if (level == minecraft.level && response.sequence() > lastResponse && response.sequence() <= sequence) {
            lastResponse = response.sequence();
            text = response.target().filter(value -> value.equals(target) && value.equals(target(minecraft)))
                    .map(value -> response.text()).orElseGet(List::of);
        }
    }

    public static void draw(GuiGraphics graphics, int offset) {
        var minecraft = Minecraft.getInstance();
        if (text.isEmpty() || target == null || !target.equals(target(minecraft))) {
            return;
        }
        int width = graphics.guiWidth();
        int y = graphics.guiHeight() / 2 + 20 + offset;
        for (var line : text) {
            for (var wrapped : minecraft.font.split(line, width - 16)) {
                if (y + minecraft.font.lineHeight > graphics.guiHeight() - 38) {
                    return;
                }
                graphics.drawCenteredString(minecraft.font, wrapped, width / 2, y, 0xFFFFFF);
                y += minecraft.font.lineHeight;
            }
        }
    }

    private static @Nullable FurnitureReading.Target target(Minecraft minecraft) {
        var player = minecraft.player;
        if (player == null || minecraft.level == null || minecraft.screen != null || minecraft.options.hideGui
                || !minecraft.options.getCameraType().isFirstPerson() || minecraft.getCameraEntity() != player
                || player.isSpectator() || !ReadingEnchantment.canRead(player.getItemBySlot(EquipmentSlot.HEAD))
                || !(minecraft.hitResult instanceof BlockHitResult hit)) {
            return null;
        }
        return FurnitureReading.target(minecraft.level, hit);
    }
}
