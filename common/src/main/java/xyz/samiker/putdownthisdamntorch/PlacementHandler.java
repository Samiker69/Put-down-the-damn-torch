package xyz.samiker.putdownthisdamntorch;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import xyz.samiker.putdownthisdamntorch.config.ModConfig;

public class PlacementHandler {

    public static boolean tryPlaceLight(Minecraft client, BlockPos pos, InventoryHandler.ItemResult itemInfo) {
        if (client.gameMode == null || client.level == null || client.player == null) return false;
        if (client.player.isInWater() && !ModConfig.worksInWater) return false;

        if (!(itemInfo.item() instanceof BlockItem blockItem)) return false;

        BlockState state = blockItem.getBlock().defaultBlockState();
        if (!state.canSurvive(client.level, pos)) {
            return false;
        }

        int prevSlot = client.player.getInventory().getSelectedSlot();
        if (itemInfo.requiresSwap()) {
            client.player.getInventory().setSelectedSlot(itemInfo.slotIndex());
        }

        BlockHitResult hitResult = new BlockHitResult(
                Vec3.atCenterOf(pos),
                Direction.UP,
                pos.below(),
                false
        );

        InteractionHand hand = itemInfo.requiresSwap() ? InteractionHand.MAIN_HAND : itemInfo.hand();

        var result = client.gameMode.useItemOn(client.player, hand, hitResult);

        if (itemInfo.requiresSwap()) {
            client.player.getInventory().setSelectedSlot(prevSlot);
        }

        if (result.consumesAction()) {
            client.player.swing(hand);
            return true;
        }

        return false;
    }
}