package art.arcane.bibliocraft.menu;

import art.arcane.bibliocraft.config.BiblioConfig;
import art.arcane.bibliocraft.item.WaypointCompassItem;
import art.arcane.bibliocraft.registry.ModMenus;
import net.minecraft.SharedConstants;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;

public final class WaypointCompassMenu extends HeldItemMenu {
    private final WaypointCompassItem.Target target;

    public WaypointCompassMenu(int id, Inventory inventory, FriendlyByteBuf buffer) {
        this(id, inventory.player, buffer.readEnum(InteractionHand.class), WaypointCompassItem.Target.read(buffer));
    }

    public WaypointCompassMenu(int id, Player owner, InteractionHand hand, WaypointCompassItem.Target target) {
        super(ModMenus.WAYPOINT_COMPASS.get(), id, owner, hand);
        this.target = target;
    }

    public WaypointCompassItem.Target target() {
        return target;
    }

    public boolean save(Player player, int x, int z, String name, boolean currentDimension) {
        if (!stillValid(player) || !(heldStack.getItem() instanceof WaypointCompassItem)
                || !BiblioConfig.COMMON.enableWaypointCompass.get()
                || !WaypointCompassItem.validCoordinate(x) || !WaypointCompassItem.validCoordinate(z)) {
            return false;
        }
        // Merely renaming a waypoint leaves its dimension intact.
        var dimension = !currentDimension && x == target.x() && z == target.z()
                ? target.dimension() : player.level().dimension().location();
        WaypointCompassItem.setTarget(heldStack, new WaypointCompassItem.Target(x, z,
                SharedConstants.filterText(name).strip(), dimension));
        player.getInventory().setChanged();
        return true;
    }
}
