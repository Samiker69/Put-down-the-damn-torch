package xyz.samiker.putdownthisdamntorch;

import xyz.samiker.putdownthisdamntorch.config.ModConfig;

public enum Mode {
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
            case ALWAYS -> ModConfig.ModeSettings.alwaysLightThreshold;
            case NIGHT -> ModConfig.ModeSettings.nightLightThreshold;
            case CAVE -> ModConfig.ModeSettings.caveLightThreshold;
        };
    }

    public boolean check(int light, long time) {
        boolean isNight = time >= 13000 && time <= 23000;
        boolean isDarkEnough = light <= this.getConfigLightThreshold();

        return switch (this) {
            case ALWAYS -> true;
            case NIGHT -> isNight && isDarkEnough;
            case CAVE -> isDarkEnough;
        };
    }
}