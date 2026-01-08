package xyz.samiker.putdownthisdamntorch;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import xyz.samiker.putdownthisdamntorch.config.ModConfig;

public class InventoryHandler {

    public record ItemResult(Item item, InteractionHand hand, int slotIndex, boolean requiresSwap) {}

    public static ItemResult findLightSource(Player player) {
        for (String idStr : ModConfig.prioritizedLightSources) {
            ResourceLocation id = ResourceLocation.tryParse(idStr);
            if (id == null) continue;

            Item targetItem = BuiltInRegistries.ITEM.getValue(id);
            if (targetItem == null) continue;

            if (player.getOffhandItem().is(targetItem)) {
                return new ItemResult(targetItem, InteractionHand.OFF_HAND, -1, false);
            }

            if (player.getMainHandItem().is(targetItem)) {
                return new ItemResult(targetItem, InteractionHand.MAIN_HAND, player.getInventory().getSelectedSlot(), false);
            }

            if (ModConfig.enableHotbarSwitch) {
                for (int i = 0; i < 9; i++) {
                    ItemStack stack = player.getInventory().getItem(i);
                    if (stack.is(targetItem)) {
                        return new ItemResult(targetItem, InteractionHand.MAIN_HAND, i, true);
                    }
                }
            }
        }
        return null;
    }
}