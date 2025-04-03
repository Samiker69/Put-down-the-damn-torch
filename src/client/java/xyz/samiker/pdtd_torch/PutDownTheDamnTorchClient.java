package xyz.samiker.pdtd_torch;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import xyz.samiker.pdtd_torch.config.PdtdConfig;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PutDownTheDamnTorchClient implements ClientModInitializer {
    private static PdtdConfig CONFIG;

	private static KeyBinding toggleKey;
	private static boolean enabled = false;

	private static Mode currentMode = Mode.ALWAYS;

    private enum Mode {
        ALWAYS("Always"),
        NIGHT("Night Mode"),
        CAVE("Cave Mode");

		private final String displayName;

		Mode(String displayName) {
			this.displayName = displayName;
		}

		public String getDisplayName() { return displayName; }

		// Метод для получения следующего режима по кругу
		public Mode next() {
			Mode[] modes = values(); // Массив всех значений enum [ALWAYS, NIGHT, CAVE]
			int nextOrdinal = (this.ordinal() + 1) % modes.length; // Индекс следующего + зацикливание
			return modes[nextOrdinal];
		}
        public int getConfigLightThreshold() {
            return switch (this) {
                case ALWAYS -> CONFIG.modes.alwaysLightThreshold;
                case NIGHT  -> CONFIG.modes.nightLightThreshold;
                case CAVE   -> CONFIG.modes.caveLightThreshold;
            };
        }
	}

	private static long lastPlacementTick = 0;
	public static final Logger LOGGER = LoggerFactory.getLogger("pdtd_torch");

	@Override
	public void onInitializeClient() {
		LOGGER.info("it's work :3");

        AutoConfig.register(PdtdConfig.class, GsonConfigSerializer::new);
        CONFIG = AutoConfig.getConfigHolder(PdtdConfig.class).getConfig();

		toggleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
				"key.pdtd_torch.toggle",
				InputUtil.Type.KEYSYM,
				GLFW.GLFW_KEY_N,
				"category.pdtd_torch"
		));

		KeyBinding modeKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
				"key.pdtd_torch.mode",
				InputUtil.Type.KEYSYM,
				GLFW.GLFW_KEY_V,
				"category.pdtd_torch"
		));

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
		});

		HudRenderCallback.EVENT.register((drawContext, tickDelta) -> {
			if (enabled) {
				TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
				drawContext.drawText(textRenderer, "Auto Torch: §aEnabled", CONFIG.hud.hudX, CONFIG.hud.hudY, 0xFFFFFF, true);
				drawContext.drawText(textRenderer, "Mode: §a" + currentMode.getDisplayName(), CONFIG.hud.hudX, CONFIG.hud.hudY + 10, 0xFFFFFF, true);
			}
		});

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (enabled && client.player != null && client.world != null) {

				long currentTime = client.world.getTime();
				if (currentTime < lastPlacementTick +  CONFIG.placementCooldownTicks) {
					return; // Еще не прошло достаточно времени с последней установки
				}

				BlockPos playerPos = client.player.getBlockPos(); // Позиция ног игрока
				int lightLevel = client.world.getLightLevel(playerPos);
				long timeOfDay = client.world.getTimeOfDay() % 24000;

                if (shouldPlaceTorch(lightLevel, timeOfDay, currentMode) && hasTorch(client.player)) {
                    placeTorch(client, playerPos);
                }
			}
		});
	}

	private static boolean shouldPlaceTorch(int lightLevel, long timeOfDay, Mode mode) {
		boolean isNight = timeOfDay >= 13000 && timeOfDay <= 23000;
		boolean isDarkEnough = lightLevel <= mode.getConfigLightThreshold();

		return switch (mode) {
			case ALWAYS -> isDarkEnough;
			case NIGHT -> isNight && isDarkEnough;
			case CAVE -> isDarkEnough;
		};
	}

	private static boolean hasTorch(PlayerEntity player) {
		if (player.getOffHandStack().getItem() == Items.TORCH) {
			return true;
		}
		for (ItemStack stack : player.getInventory().main) {
			if (stack.getItem() == Items.TORCH) {
				return true;
			}
		}
		return false;
	}

	private static void placeTorch(MinecraftClient client, BlockPos pos) {
		if (client.interactionManager == null || client.world == null || client.player == null) {
			return;
		}

		BlockState torchState = Blocks.TORCH.getDefaultState();
		if (!torchState.canPlaceAt(client.world, pos)) {
			// LOGGER.debug("Cannot place torch at {}", pos); // для отладки
			return;
		}
		Hand handToUse = null;
        int originalSlot = client.player.getInventory().selectedSlot;
        boolean switchedSlot = false;

        if (client.player.getOffHandStack().getItem() == Items.TORCH) {
            handToUse = Hand.OFF_HAND;
        } else if (client.player.getMainHandStack().getItem() == Items.TORCH) {
            handToUse = Hand.MAIN_HAND;
        } else if (CONFIG.enableHotbarSwitch) {
            for (int i = 0; i < 9; i++) {
                if (client.player.getInventory().getStack(i).getItem() == Items.TORCH) {
                    client.player.getInventory().selectedSlot = i;
                    handToUse = Hand.MAIN_HAND;
                    switchedSlot = true;
                    break;
                }
            }
        }

		// Если не нашли факел ни в руках, ни в хотбаре - выходим
        if (handToUse == null) return;

        BlockHitResult hitResult = new BlockHitResult(Vec3d.ofCenter(pos), Direction.UP, pos, false);
        ActionResult result = client.interactionManager.interactBlock(client.player, handToUse, hitResult);

        if (result.isAccepted()) {
            client.player.swingHand(handToUse);
            lastPlacementTick = client.world.getTime();
        } else {
            LOGGER.warn("Failed to place torch at {} using hand {}. Result: {}", pos, handToUse, result);
        }

        if (switchedSlot) {
            client.player.getInventory().selectedSlot = originalSlot;
        }
	}

    // hm, idk :3
    public static PdtdConfig getConfig() {
        return CONFIG;
    }
}