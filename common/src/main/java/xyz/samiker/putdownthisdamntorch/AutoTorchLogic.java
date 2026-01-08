package xyz.samiker.putdownthisdamntorch;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.client.Minecraft;
import xyz.samiker.putdownthisdamntorch.config.ModConfig;

public class AutoTorchLogic {
    private static boolean enabled = false;
    private static Mode currentMode = Mode.CAVE;
    private static long lastPlacementTick = 0;

    public static void toggleEnabled(Player player) {
        enabled = !enabled;
        player.displayClientMessage(Component.literal("Auto Torch: " + (enabled ? "§aEnabled" : "§cDisabled")), true);
    }

    public static void cycleMode(Player player) {
        currentMode = currentMode.next();
        player.displayClientMessage(Component.literal("Torch Mode: §a" + currentMode.getDisplayName()), true);
    }

    public static void reset() {
        lastPlacementTick = 0;
    }

    public static void onJoin() {
        lastPlacementTick = 0;
        if (ModConfig.forgetSettingsWhenLeavingWorld) {
            enabled = false;
            currentMode = Mode.CAVE;
        }
    }

    public static void tick(Minecraft client) {
        if (!enabled || client.player == null) return;

        assert client.level != null;
        long currentTime = client.level.getGameTime();
        if (currentTime < lastPlacementTick + ModConfig.placementCooldownTicks) return;

        BlockPos pos = client.player.blockPosition();
        int lightLevel = client.level.getMaxLocalRawBrightness(pos);
        long timeOfDay = client.level.getDayTime() % 24000;

        if (shouldPlace(lightLevel, timeOfDay)) {
            var itemInfo = InventoryHandler.findLightSource(client.player);

            if (itemInfo != null) {
                // Делегируем установку
                boolean success = PlacementHandler.tryPlaceLight(client, pos, itemInfo);
                if (success) {
                    lastPlacementTick = currentTime;
                }
            }
        }
    }

    private static boolean shouldPlace(int light, long time) {
        return currentMode.check(light, time);
    }

    // Enum Mode можно перенести внутрь или в отдельный файл
}