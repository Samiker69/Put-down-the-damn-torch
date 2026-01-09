package xyz.samiker.putdownthisdamntorch;

import dev.architectury.event.events.client.ClientPlayerEvent;
import dev.architectury.event.events.client.ClientTickEvent;
import dev.architectury.event.events.client.ClientGuiEvent;
import dev.architectury.registry.client.keymappings.KeyMappingRegistry;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.glfw.GLFW;
import xyz.samiker.putdownthisdamntorch.config.ModConfig;

public final class PutDownThisDamnTorch {
    public static final String MOD_ID = "putdownthisdamntorch";
    private static final KeyMapping.Category CATEGORY =  KeyMapping.Category.register(ResourceLocation.parse(MOD_ID));

    public static final KeyMapping TOGGLE_KEY = new KeyMapping(
            "key.putdownthisdamntorch.toggle",
            GLFW.GLFW_KEY_N,
            CATEGORY
    );

    public static final KeyMapping MODE_KEY = new KeyMapping(
            "key.putdownthisdamntorch.mode",
            GLFW.GLFW_KEY_V,
            CATEGORY
    );

    public static final KeyMapping CONFIG_KEY = new KeyMapping(
            "key.putdownthisdamntorch.config",
            GLFW.GLFW_KEY_B,
            CATEGORY
    );

    public static void init() {
        AutoConfig.register(ModConfig.class, GsonConfigSerializer::new);
        KeyMappingRegistry.register(TOGGLE_KEY);
        KeyMappingRegistry.register(MODE_KEY);

        ClientTickEvent.CLIENT_POST.register(PutDownThisDamnTorch::onTick);
        ClientPlayerEvent.CLIENT_PLAYER_QUIT.register(player -> AutoTorchLogic.reset());
        ClientPlayerEvent.CLIENT_PLAYER_JOIN.register(player -> AutoTorchLogic.onJoin());
    }

    private static void onTick(Minecraft client) {
        if (client.player == null || client.level == null) return;
        while (TOGGLE_KEY.consumeClick()) {
            AutoTorchLogic.toggleEnabled(client.player);
        }
        while (MODE_KEY.consumeClick()) {
            AutoTorchLogic.cycleMode(client.player);
        }
        while (CONFIG_KEY.consumeClick()) {
            ModConfig config = AutoConfig.getConfigHolder(ModConfig.class).getConfig();
            client.setScreen(new ModConfigScreen(client.screen, config));
        }
        AutoTorchLogic.tick(client);
    }
}
