package art.arcane.bibliocraft.event;

import art.arcane.bibliocraft.BiblioCraft;
import art.arcane.bibliocraft.catalog.CatalogContents;
import art.arcane.bibliocraft.config.BiblioConfig;
import art.arcane.bibliocraft.item.StockroomCatalogItem;
import art.arcane.bibliocraft.registry.ModParticles;
import art.arcane.bibliocraft.block.ArmorStandBlock;
import art.arcane.bibliocraft.config.BiblioFeatureToggles;
import art.arcane.bibliocraft.block.BookcaseBlock;
import art.arcane.bibliocraft.block.ClockBlock;
import art.arcane.bibliocraft.block.DinnerPlateBlock;
import art.arcane.bibliocraft.block.DiscRackBlock;
import art.arcane.bibliocraft.block.DisplayCaseBlock;
import art.arcane.bibliocraft.block.FurniturePanelerBlock;
import art.arcane.bibliocraft.block.FancyWorkbenchBlock;
import art.arcane.bibliocraft.block.RackBlock;
import art.arcane.bibliocraft.block.SeatBlock;
import art.arcane.bibliocraft.block.ShelfBlock;
import art.arcane.bibliocraft.block.SignDisplayBlock;
import art.arcane.bibliocraft.block.TableBlock;
import art.arcane.bibliocraft.block.TypewriterBlock;
import art.arcane.bibliocraft.block.DeskBlock;
import art.arcane.bibliocraft.block.MapFrameBlock;
import art.arcane.bibliocraft.blockentity.FurnitureBlockEntity;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.item.ItemTossEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = BiblioCraft.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class CommonGameplayEvents {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final Set<String> CONTAINER_SENSITIVE_ITEMS = new HashSet<>();
    private static final Map<UUID, BlockPos> PENDING_DEATH_LOCATIONS = new HashMap<>();

    static {
        // Legacy toss flow closed packet-heavy item containers before dropping.
        CONTAINER_SENSITIVE_ITEMS.add(BiblioCraft.MODID + ":atlas_book");
        CONTAINER_SENSITIVE_ITEMS.add(BiblioCraft.MODID + ":big_book");
        CONTAINER_SENSITIVE_ITEMS.add(BiblioCraft.MODID + ":recipe_book");
        CONTAINER_SENSITIVE_ITEMS.add(BiblioCraft.MODID + ":biblio_clipboard");
        CONTAINER_SENSITIVE_ITEMS.add(BiblioCraft.MODID + ":biblio_red_book");
        CONTAINER_SENSITIVE_ITEMS.add(BiblioCraft.MODID + ":slotted_book");
        CONTAINER_SENSITIVE_ITEMS.add(BiblioCraft.MODID + ":compass");
    }

    private CommonGameplayEvents() {}

    @SubscribeEvent
    public static void onLockedFurnitureBreak(BlockEvent.BreakEvent event) {
        var furniture = FurnitureBlockEntity.findAt(event.getLevel(), event.getPos());
        if (!event.getPlayer().isCreative() && furniture != null && furniture.isLocked()) {
            event.setCanceled(true);
            event.getPlayer().displayClientMessage(Component.translatable("message.bibliocraft.lock.break"), true);
        }
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            // Placeholder for death compass behavior parity.
            PENDING_DEATH_LOCATIONS.put(player.getUUID(), player.blockPosition());
        }
    }

    @SubscribeEvent
    public static void onPlayerSpawn(EntityJoinLevelEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            BlockPos deathPos = PENDING_DEATH_LOCATIONS.remove(player.getUUID());
            if (deathPos != null) {
                // Placeholder hook for restoring death waypoint data to the player.
                LOGGER.debug("Queued death position for {} at {}", player.getGameProfile().getName(), deathPos);
            }
        }
    }

    @SubscribeEvent
    public static void onItemToss(ItemTossEvent event) {
        Player player = event.getPlayer();
        if (player == null || player.level().isClientSide()) {
            return;
        }

        ItemStack tossed = event.getEntity().getItem();
        ResourceLocation key = ForgeRegistries.ITEMS.getKey(tossed.getItem());
        if (key != null
                && BiblioFeatureToggles.isItemEnabled(key)
                && CONTAINER_SENSITIVE_ITEMS.contains(key.toString())) {
            // Placeholder for legacy packet/container validation before toss.
            player.closeContainer();
        }
    }

    @SubscribeEvent
    public static void onCatalogHeld(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)
                || player.tickCount % 15 != 0 || !BiblioConfig.COMMON.enableStockroomCatalog.get()) {
            return;
        }
        ItemStack catalog = player.getMainHandItem();
        if (!(catalog.getItem() instanceof StockroomCatalogItem)) {
            catalog = player.getOffhandItem();
        }
        if (!(catalog.getItem() instanceof StockroomCatalogItem)) {
            return;
        }
        var level = player.serverLevel();
        for (var location : CatalogContents.locations(catalog, level.dimension())) {
            BlockPos pos = location.pos();
            if (!location.dimension().equals(level.dimension()) || !pos.closerToCenterThan(player.position(), 24)
                    || !level.hasChunkAt(pos) || CatalogContents.findContainer(level, pos) == null) {
                continue;
            }
            int direction = player.tickCount / 15 % 5;
            double dx = direction == 0 ? 0.07 : direction == 1 ? -0.07 : 0.01;
            double dy = direction == 2 ? 0.07 : 0.01;
            double dz = direction == 3 ? 0.07 : direction == 4 ? -0.07 : 0.01;
            level.sendParticles(player, ModParticles.CATALOG.get(), false, pos.getX() + 0.5,
                    pos.getY() + 0.5, pos.getZ() + 0.5, 0, dx, dy, dz, 1.0);
        }
    }

    @SubscribeEvent
    public static void onFurnitureSecondaryUse(PlayerInteractEvent.RightClickBlock event) {
        var block = event.getLevel().getBlockState(event.getPos()).getBlock();
        boolean workbenchExtraction = block instanceof FancyWorkbenchBlock && event.getItemStack().isEmpty();
        if (event.getHand() == InteractionHand.MAIN_HAND && event.getEntity().isShiftKeyDown()
                && event.getUseBlock() == Event.Result.DEFAULT
                && (block instanceof ShelfBlock || block instanceof BookcaseBlock || block instanceof DisplayCaseBlock
                        || block instanceof RackBlock || workbenchExtraction || block instanceof FurniturePanelerBlock
                        || block instanceof DinnerPlateBlock || block instanceof DiscRackBlock || block instanceof TypewriterBlock
                        || block instanceof DeskBlock || block instanceof ArmorStandBlock || block instanceof TableBlock
                        || block instanceof SeatBlock || block instanceof SignDisplayBlock || block instanceof ClockBlock
                        || block instanceof MapFrameBlock)
                && BiblioFeatureToggles.isBlockEnabled(ForgeRegistries.BLOCKS.getKey(block))) {
            event.setUseBlock(Event.Result.ALLOW);
        }
    }

}
