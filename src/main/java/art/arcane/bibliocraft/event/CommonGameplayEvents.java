package art.arcane.bibliocraft.event;

import art.arcane.bibliocraft.BiblioCraft;
import art.arcane.bibliocraft.entity.SeatEntity;
import art.arcane.bibliocraft.registry.ModBlocks;
import art.arcane.bibliocraft.registry.ModEntities;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.item.ItemTossEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
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
        if (key != null && CONTAINER_SENSITIVE_ITEMS.contains(key.toString())) {
            // Placeholder for legacy packet/container validation before toss.
            player.closeContainer();
        }
    }

    @SubscribeEvent
    public static void onSeatInteract(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        if (event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }

        Player player = event.getEntity();
        if (player.isSecondaryUseActive() || player.isPassenger()) {
            return;
        }
        if (!level.getBlockState(event.getPos()).is(ModBlocks.SEAT.get())) {
            return;
        }

        SeatEntity seat = findOrCreateSeat(level, event.getPos());
        if (seat == null || !seat.getPassengers().isEmpty()) {
            return;
        }

        if (player.startRiding(seat, true)) {
            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);
        }
    }

    private static SeatEntity findOrCreateSeat(ServerLevel level, BlockPos seatPos) {
        AABB searchBox = new AABB(seatPos).inflate(0.25D);
        for (SeatEntity seat : level.getEntitiesOfClass(SeatEntity.class, searchBox)) {
            if (!seat.isRemoved() && seat.isForSeat(seatPos)) {
                return seat;
            }
        }

        SeatEntity seat = ModEntities.SEAT_ENTITY.get().create(level);
        if (seat == null) {
            return null;
        }

        seat.setSeatPos(seatPos);
        level.addFreshEntity(seat);
        return seat;
    }
}
