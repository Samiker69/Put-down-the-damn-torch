package xyz.samiker.pdtd_torch;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.block.BlockState;
import net.minecraft.block.Block;
import net.minecraft.block.TorchBlock;
import net.minecraft.block.WallTorchBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.BlockItem;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.samiker.pdtd_torch.config.ModConfig;

public class PutDownTheDamnTorchClient implements ClientModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("pdtd_torch");
    private static ModConfig CONFIG;
    private static KeyBinding toggleKey;
    private static boolean enabled = false;
    private static Mode currentMode = Mode.CAVE;
    private static long lastPlacementTick = 0;

	private record FoundItemInfo(Item item, Hand hand, int hotbarSlot, boolean needsSwitch) {
    }

	private static FoundItemInfo findHighestPriorityLightSource(PlayerEntity player) {
		// 1. Итерация по списку из конфига
		for (String itemIdStr : CONFIG.prioritizedLightSources) {
			Identifier itemId = Identifier.tryParse(itemIdStr);
			if (itemId == null) {
				LOGGER.warn("Invalid item ID in config: {}", itemIdStr);
				continue;
			}

			Item item = Registries.ITEM.getOrEmpty(itemId).orElse(null);
			 if (item == null) {
                 LOGGER.debug("Item not found in registry: {}", itemIdStr);
				 continue;
			 }

			if (player.getOffHandStack().getItem() == item) {
				return new FoundItemInfo(item, Hand.OFF_HAND, -1, false);
			}

			if (player.getMainHandStack().getItem() == item) {
				return new FoundItemInfo(item, Hand.MAIN_HAND, player.getInventory().selectedSlot, false);
			}

			if (CONFIG.enableHotbarSwitch) {
				for (int i = 0; i < 9; i++) {
					if (i == player.getInventory().selectedSlot) continue;

					if (player.getInventory().getStack(i).getItem() == item) {
						return new FoundItemInfo(item, Hand.MAIN_HAND, i, true); // Нашли в хотбаре, нужно переключить
					}
				}
			}
		}
		return null;
	}

    private static boolean shouldPlaceLightSource(int lightLevel, long timeOfDay, Mode mode) {
        boolean isNight = timeOfDay >= 13000 && timeOfDay <= 23000;
        boolean isDarkEnough = lightLevel <= mode.getConfigLightThreshold();

        return switch (mode) {
            case ALWAYS -> true;
            case NIGHT -> isNight && isDarkEnough;
            case CAVE -> isDarkEnough;
        };
    }

    private static void placeLightSource(MinecraftClient client, BlockPos pos, FoundItemInfo itemInfo) {
        if (client.interactionManager == null || client.world == null || client.player == null) {
            return;
        }
        if (client.player.isTouchingWater() && !CONFIG.worksInWater) return;

        Item itemToPlace = itemInfo.item();
        if (!(itemToPlace instanceof BlockItem blockItem)) {
            LOGGER.warn("Item {} is not a BlockItem, cannot place automatically.", Registries.ITEM.getId(itemToPlace));
            return; // Не можем разместить предмет, который не является блоком
        }

        Block blockToPlace = blockItem.getBlock();
        BlockState stateToPlace = blockToPlace.getDefaultState();
        boolean isTorchLike = blockToPlace instanceof TorchBlock || blockToPlace instanceof WallTorchBlock;

        // --- Логика проверки места установки ---
        boolean canPlace = false;
        if (isTorchLike) {
            canPlace = stateToPlace.canPlaceAt(client.world, pos);
        }

        if (!canPlace) {
            // LOGGER.debug("Cannot place {} at {}", Registries.BLOCK.getId(blockToPlace), pos); // для отладки
            return;
        }

        // --- Логика переключения хотбара и взаимодействия ---
        Hand handToUse = itemInfo.hand();
        int originalSlot = client.player.getInventory().selectedSlot;
        boolean switchedSlot = itemInfo.needsSwitch();

        if (switchedSlot) {
            if (itemInfo.hotbarSlot() < 0 || itemInfo.hotbarSlot() >= 9) {
                 LOGGER.error("Invalid hotbar slot index {} for item {}", itemInfo.hotbarSlot(), Registries.ITEM.getId(itemToPlace));
                 return; // Ошибка в логике поиска
            }
            client.player.getInventory().selectedSlot = itemInfo.hotbarSlot();
            // Важно: рука будет MAIN_HAND, т.к. мы переключили слот хотбара
            handToUse = Hand.MAIN_HAND;
        }
        BlockHitResult hitResult = new BlockHitResult(
                Vec3d.ofCenter(pos.down()).add(0, 0.5, 0), // Центр верхней грани блока под нами
                Direction.UP, // Направление взгляда вверх (относительно блока под нами)
                pos.down(), // Блок, по которому кликаем
                false // Не внутри блока
        );

        ActionResult result = client.interactionManager.interactBlock(client.player, handToUse, hitResult);

        if (result.isAccepted()) {
            client.player.swingHand(handToUse);
            lastPlacementTick = client.world.getTime();
            // LOGGER.debug("Placed {} at {}", Registries.BLOCK.getId(blockToPlace), pos);
        } else {
            // LOGGER.warn("Failed to place {} at {} using hand {}. Result: {}", Registries.BLOCK.getId(blockToPlace), pos, handToUse, result);
            // Возможно, стоит попробовать кликнуть по другой грани или использовать другую логику hitResult для некоторых блоков?
        }

        // Возвращаем оригинальный слот хотбара, если переключали
        if (switchedSlot) {
            client.player.getInventory().selectedSlot = originalSlot;
        }
    }

    // idk. why not :3
    public static ModConfig getConfig() {
        return CONFIG;
    }

    @Override
    public void onInitializeClient() {
        LOGGER.info("it's work :3");

        AutoConfig.register(ModConfig.class, GsonConfigSerializer::new);
        CONFIG = AutoConfig.getConfigHolder(ModConfig.class).getConfig();

        toggleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.pdtd_torch.toggle", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_N, "category.pdtd_torch"));

        KeyBinding modeKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.pdtd_torch.mode", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_V, "category.pdtd_torch"));

        ClientPlayConnectionEvents.JOIN.register((handler, client, sender) -> {
            lastPlacementTick = 0;
            if (CONFIG.forgetSettingsWhenLeavingWorld) {
                enabled = false;
                currentMode = Mode.CAVE;
            }
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            lastPlacementTick = 0;
        });


        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (toggleKey.wasPressed()) {
                enabled = !enabled;
                if (client.player != null) {
                    client.player.sendMessage(Text.of("Auto Torch: " + (enabled ? "§aEnabled" : "§cDisabled")), true);
                }
            }
            while (modeKey.wasPressed()) {
                currentMode = currentMode.next(); // Получаем следующий режим
                if (client.player != null) {
                    client.player.sendMessage(Text.of("Torch Mode: §a" + currentMode.getDisplayName()), true);
                }
            }

            if (enabled && client.player != null && client.world != null) {

                long currentTime = client.world.getTime();
                if (currentTime < lastPlacementTick + CONFIG.placementCooldownTicks) {
                    return; // Кулдаун
                }

                BlockPos playerPos = client.player.getBlockPos(); // Позиция ног игрока
                int lightLevel = client.world.getLightLevel(playerPos);
                long timeOfDay = client.world.getTimeOfDay() % 24000;

                // Проверяем, нужно ли ставить свет по условиям режима
                if (shouldPlaceLightSource(lightLevel, timeOfDay, currentMode)) {
                    // Ищем подходящий предмет в инвентаре по приоритету
                    FoundItemInfo itemInfo = findHighestPriorityLightSource(client.player);
                    if (itemInfo != null) {
                        // Если нашли предмет, пытаемся его разместить в позиции ног игрока
                        placeLightSource(client, playerPos, itemInfo);
                    }
                }
            }
        });

        HudRenderCallback.EVENT.register((drawContext, tickDelta) -> {
            if (enabled) {
                TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
                drawContext.drawText(textRenderer, "Auto Torch: §aEnabled", CONFIG.hud.hudX, CONFIG.hud.hudY, 0xFFFFFF, true);
                drawContext.drawText(textRenderer, "Mode: §a" + currentMode.getDisplayName(), CONFIG.hud.hudX, CONFIG.hud.hudY + 10, 0xFFFFFF, true);
            }
        });
    }

    private enum Mode {
        ALWAYS("Always"), NIGHT("Night Mode"), CAVE("Cave Mode");

        private final String displayName;

        Mode(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }

        public Mode next() {
            Mode[] modes = values();
            int nextOrdinal = (this.ordinal() + 1) % modes.length; 
            return modes[nextOrdinal];
        }

        public int getConfigLightThreshold() {
            return switch (this) {
                case ALWAYS -> CONFIG.modes.alwaysLightThreshold;
                case NIGHT -> CONFIG.modes.nightLightThreshold;
                case CAVE -> CONFIG.modes.caveLightThreshold;
            };
        }
    }
}