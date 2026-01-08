package xyz.samiker.putdownthisdamntorch.fabric;

import xyz.samiker.putdownthisdamntorch.PutDownThisDamnTorch;
import net.fabricmc.api.ModInitializer;

public final class PutdownthisdamntorchFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        PutDownThisDamnTorch.init();
    }
}
