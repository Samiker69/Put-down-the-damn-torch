package xyz.samiker.pdtd_torch.config;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;
import me.shedaniel.cloth.clothconfig.shadowed.blue.endless.jankson.Comment;

@Config(name = "PutDownTheDamnTorch")
public class ModConfig implements ConfigData {

    @ConfigEntry.Gui.CollapsibleObject
    @Comment("HUD Settings")
    public HudSettings hud = new HudSettings();

    @ConfigEntry.Gui.CollapsibleObject
    @Comment("Mode Settings")
    public ModeSettings modes = new ModeSettings();

    @Comment("Enable automatically switching to a torch in the hotbar if not in hand?")
    public boolean enableHotbarSwitch = true;

    @Comment("Cooldown between automatic torch placements (in ticks, 20 ticks = 1 second)")
    @ConfigEntry.BoundedDiscrete(min = 0, max = 100) // Ограничение значения в GUI
    public int placementCooldownTicks = 10;

    @Comment("Allow placing torches while the player is touching water?")
    public boolean worksInWater = false;

    public static class HudSettings {
        @Comment("X position of the HUD text")
        public int hudX = 10;

        @Comment("Y position of the HUD text")
        public int hudY = 12;
    }

    public static class ModeSettings {
        @Comment("Light level threshold for 'Always' mode")
        @ConfigEntry.BoundedDiscrete(min = 0, max = 15)
        public int alwaysLightThreshold = 9;

        @Comment("Light level threshold for 'Night' mode")
        @ConfigEntry.BoundedDiscrete(min = 0, max = 15)
        public int nightLightThreshold = 6;

        @Comment("Light level threshold for 'Cave' mode")
        @ConfigEntry.BoundedDiscrete(min = 0, max = 15)
        public int caveLightThreshold = 5;
    }
}