package xyz.samiker.putdownthisdamntorch.neoforge;

import me.shedaniel.autoconfig.AutoConfig;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import xyz.samiker.putdownthisdamntorch.PutDownThisDamnTorch;
import net.neoforged.fml.common.Mod;
import xyz.samiker.putdownthisdamntorch.config.ModConfig;

@Mod(PutDownThisDamnTorch.MOD_ID)
public final class PutdownthisdamntorchNeoForge {
    public PutdownthisdamntorchNeoForge() {
        PutDownThisDamnTorch.init();
        ModLoadingContext.get().registerExtensionPoint(
                IConfigScreenFactory.class,
                () -> (client, parent) -> AutoConfig.getConfigScreen(ModConfig.class, parent).get()
        );

    }
}
