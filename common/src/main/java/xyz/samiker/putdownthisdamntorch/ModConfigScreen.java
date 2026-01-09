package xyz.samiker.putdownthisdamntorch;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import xyz.samiker.putdownthisdamntorch.config.ModConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ModConfigScreen extends Screen {
    private final Screen parent;
    private final ModConfig config;

    private enum Tab { GENERAL, HUD, MODES, LIGHT_SOURCES }
    private Tab currentTab = Tab.GENERAL;

    private List<String> tempLightSources;
    private int scrollOffset = 0;
    private static final int ITEMS_PER_PAGE = 5;

    private boolean tempForgetSettings;
    private boolean tempEnableHotbarSwitch;
    private int tempPlacementCooldown;
    private boolean tempWorksInWater;
    private int tempHudX;
    private int tempHudY;
    private int tempAlwaysThreshold;
    private int tempNightThreshold;
    private int tempCaveThreshold;

    private EditBox newItemInput;
    private List<String> itemSuggestions = new ArrayList<>();
    private int selectedSuggestion = -1;
    private int draggingItemIndex = -1;
    private boolean isDragging = false;

    public ModConfigScreen(Screen parent, ModConfig config) {
        super(Component.literal("Put Down This Damn Torch - Config"));
        this.parent = parent;
        this.config = config;

        this.tempLightSources = new ArrayList<>(ModConfig.prioritizedLightSources);
        this.tempForgetSettings = ModConfig.forgetSettingsWhenLeavingWorld;
        this.tempEnableHotbarSwitch = ModConfig.enableHotbarSwitch;
        this.tempPlacementCooldown = ModConfig.placementCooldownTicks;
        this.tempWorksInWater = ModConfig.worksInWater;
        this.tempHudX = ModConfig.HudSettings.hudX;
        this.tempHudY = ModConfig.HudSettings.hudY;
        this.tempAlwaysThreshold = ModConfig.ModeSettings.alwaysLightThreshold;
        this.tempNightThreshold = ModConfig.ModeSettings.nightLightThreshold;
        this.tempCaveThreshold = ModConfig.ModeSettings.caveLightThreshold;
    }

    @Override
    protected void init() {
        super.init();

        int tabY = 40;
        int tabWidth = 100;
        int tabSpacing = 5;
        int startX = this.width / 2 - (tabWidth * 4 + tabSpacing * 3) / 2;

        this.addRenderableWidget(Button.builder(
                Component.literal("General"),
                btn -> {
                    currentTab = Tab.GENERAL;
                    rebuildWidgets();
                }
        ).bounds(startX, tabY, tabWidth, 20).build());

        this.addRenderableWidget(Button.builder(
                Component.literal("HUD"),
                btn -> {
                    currentTab = Tab.HUD;
                    rebuildWidgets();
                }
        ).bounds(startX + tabWidth + tabSpacing, tabY, tabWidth, 20).build());

        this.addRenderableWidget(Button.builder(
                Component.literal("Modes"),
                btn -> {
                    currentTab = Tab.MODES;
                    rebuildWidgets();
                }
        ).bounds(startX + (tabWidth + tabSpacing) * 2, tabY, tabWidth, 20).build());

        this.addRenderableWidget(Button.builder(
                Component.literal("Light Sources"),
                btn -> {
                    currentTab = Tab.LIGHT_SOURCES;
                    rebuildWidgets();
                }
        ).bounds(startX + (tabWidth + tabSpacing) * 3, tabY, tabWidth, 20).build());

        this.addRenderableWidget(Button.builder(
                Component.literal("Save"),
                btn -> {
                    saveConfig();
                    this.minecraft.setScreen(parent);
                }
        ).bounds(this.width / 2 - 105, this.height - 30, 100, 20).build());

        this.addRenderableWidget(Button.builder(
                Component.literal("Cancel"),
                btn -> this.minecraft.setScreen(parent)
        ).bounds(this.width / 2 + 5, this.height - 30, 100, 20).build());

        initTabContent();
    }

    @Override
    public void rebuildWidgets() {
        super.rebuildWidgets();
    }

    private void initTabContent() {
        this.children().removeIf(widget -> widget instanceof EditBox);

        int contentY = 80;
        int leftX = this.width / 2 - 150;

        switch (currentTab) {
            case GENERAL:
                initGeneralTab(leftX, contentY);
                break;
            case HUD:
                initHudTab(leftX, contentY);
                break;
            case MODES:
                initModesTab(leftX, contentY);
                break;
            case LIGHT_SOURCES:
                initLightSourcesTab(leftX, contentY);
                break;
        }
    }

    private void initGeneralTab(int x, int y) {
        this.addRenderableWidget(Button.builder(
                Component.literal("Forget settings on exit: " + (tempForgetSettings ? "YES" : "NO")),
                btn -> {
                    tempForgetSettings = !tempForgetSettings;
                    btn.setMessage(Component.literal("Forget settings on exit: " + (tempForgetSettings ? "YES" : "NO")));
                }
        ).bounds(x, y, 300, 20).build());

        this.addRenderableWidget(Button.builder(
                Component.literal("Hotbar switch: " + (tempEnableHotbarSwitch ? "ON" : "OFF")),
                btn -> {
                    tempEnableHotbarSwitch = !tempEnableHotbarSwitch;
                    btn.setMessage(Component.literal("Hotbar switch: " + (tempEnableHotbarSwitch ? "ON" : "OFF")));
                }
        ).bounds(x, y + 30, 300, 20).build());

        this.addRenderableWidget(Button.builder(
                Component.literal("Works in water: " + (tempWorksInWater ? "YES" : "NO")),
                btn -> {
                    tempWorksInWater = !tempWorksInWater;
                    btn.setMessage(Component.literal("Works in water: " + (tempWorksInWater ? "YES" : "NO")));
                }
        ).bounds(x, y + 60, 300, 20).build());

        this.addRenderableWidget(Button.builder(
                Component.literal("-"),
                btn -> {
                    tempPlacementCooldown = Math.max(0, tempPlacementCooldown - 5);
                    rebuildWidgets();
                }
        ).bounds(x, y + 90, 40, 20).build());

        this.addRenderableWidget(Button.builder(
                Component.literal("+"),
                btn -> {
                    tempPlacementCooldown = Math.min(200, tempPlacementCooldown + 5);
                    rebuildWidgets();
                }
        ).bounds(x + 260, y + 90, 40, 20).build());
    }

    private void initHudTab(int x, int y) {
        EditBox hudXBox = new EditBox(this.font, x + 100, y, 200, 20, Component.literal("HUD X"));
        hudXBox.setValue(String.valueOf(tempHudX));
        hudXBox.setResponder(val -> {
            try {
                tempHudX = Integer.parseInt(val);
            } catch (NumberFormatException ignored) {}
        });
        this.addRenderableWidget(hudXBox);

        EditBox hudYBox = new EditBox(this.font, x + 100, y + 30, 200, 20, Component.literal("HUD Y"));
        hudYBox.setValue(String.valueOf(tempHudY));
        hudYBox.setResponder(val -> {
            try {
                tempHudY = Integer.parseInt(val);
            } catch (NumberFormatException ignored) {}
        });
        this.addRenderableWidget(hudYBox);
    }

    private void initModesTab(int x, int y) {
        this.addRenderableWidget(Button.builder(
                Component.literal("-"),
                btn -> {
                    tempAlwaysThreshold = Math.max(0, tempAlwaysThreshold - 1);
                    rebuildWidgets();
                }
        ).bounds(x, y, 40, 20).build());

        this.addRenderableWidget(Button.builder(
                Component.literal("+"),
                btn -> {
                    tempAlwaysThreshold = Math.min(15, tempAlwaysThreshold + 1);
                    rebuildWidgets();
                }
        ).bounds(x + 260, y, 40, 20).build());

        this.addRenderableWidget(Button.builder(
                Component.literal("-"),
                btn -> {
                    tempNightThreshold = Math.max(0, tempNightThreshold - 1);
                    rebuildWidgets();
                }
        ).bounds(x, y + 30, 40, 20).build());

        this.addRenderableWidget(Button.builder(
                Component.literal("+"),
                btn -> {
                    tempNightThreshold = Math.min(15, tempNightThreshold + 1);
                    rebuildWidgets();
                }
        ).bounds(x + 260, y + 30, 40, 20).build());

        this.addRenderableWidget(Button.builder(
                Component.literal("-"),
                btn -> {
                    tempCaveThreshold = Math.max(0, tempCaveThreshold - 1);
                    rebuildWidgets();
                }
        ).bounds(x, y + 60, 40, 20).build());

        this.addRenderableWidget(Button.builder(
                Component.literal("+"),
                btn -> {
                    tempCaveThreshold = Math.min(15, tempCaveThreshold + 1);
                    rebuildWidgets();
                }
        ).bounds(x + 260, y + 60, 40, 20).build());
    }

    private void initLightSourcesTab(int x, int y) {
        newItemInput = new EditBox(this.font, x, y, 250, 20, Component.literal("Item ID"));
        newItemInput.setHint(Component.literal("minecraft:torch"));
        newItemInput.setResponder(this::updateSuggestions);
        this.addRenderableWidget(newItemInput);

        this.addRenderableWidget(Button.builder(
                Component.literal("+"),
                btn -> {
                    String itemId = newItemInput.getValue().trim();
                    if (!itemId.isEmpty() && isValidItem(itemId) && !tempLightSources.contains(itemId)) {
                        tempLightSources.add(itemId);
                        newItemInput.setValue("");
                        itemSuggestions.clear();
                        rebuildWidgets();
                    }
                }
        ).bounds(x + 255, y, 45, 20).build());

        if (tempLightSources.size() > ITEMS_PER_PAGE) {
            this.addRenderableWidget(Button.builder(
                    Component.literal("▲"),
                    btn -> {
                        scrollOffset = Math.max(0, scrollOffset - 1);
                    }
            ).bounds(x + 310, y + 30, 30, 20).build());

            this.addRenderableWidget(Button.builder(
                    Component.literal("▼"),
                    btn -> {
                        scrollOffset = Math.min(tempLightSources.size() - ITEMS_PER_PAGE, scrollOffset + 1);
                    }
            ).bounds(x + 310, y + 160, 30, 20).build());
        }
    }

    private void updateSuggestions(String input) {
        itemSuggestions.clear();
        selectedSuggestion = -1;

        if (input.length() >= 2) {
            itemSuggestions = BuiltInRegistries.ITEM.keySet().stream()
                    .map(ResourceLocation::toString)
                    .filter(id -> id.contains(input.toLowerCase()))
                    .filter(id -> !tempLightSources.contains(id))
                    .limit(10)
                    .collect(Collectors.toList());
        }
    }

    private boolean isValidItem(String itemId) {
        try {
            ResourceLocation loc = ResourceLocation.parse(itemId);
            return BuiltInRegistries.ITEM.containsKey(loc);
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        //this.renderBackground(graphics, mouseX, mouseY, partialTick);
        renderTabContent(graphics, mouseX, mouseY);
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 15, 0xFFFFFFFF);
        if (currentTab == Tab.LIGHT_SOURCES && !itemSuggestions.isEmpty()) {
            renderSuggestions(graphics, mouseX, mouseY);
        }
    }

    private void renderTabContent(GuiGraphics graphics, int mouseX, int mouseY) {
        int contentY = 80;
        int leftX = this.width / 2 - 150;

        switch (currentTab) {
            case GENERAL -> {
                graphics.drawString(this.font, "General Settings", leftX, contentY - 15, 0xFFFFFFFF, false);
                graphics.drawString(this.font, "Cooldown: " + tempPlacementCooldown + " ticks", leftX + 45, contentY + 95, 0xFFFFFFFF, false);
            }
            case HUD -> {
                graphics.drawString(this.font, "HUD Position:", leftX, contentY - 15, 0xFFFFFFFF, false);
                graphics.drawString(this.font, "X:", leftX, contentY + 5, 0xAAAAAAAA, false);
                graphics.drawString(this.font, "Y:", leftX, contentY + 35, 0xAAAAAAAA, false);
            }
            case MODES -> {
                graphics.drawString(this.font, "Light Level Thresholds", leftX, contentY - 15, 0xFFFFFFFF, false);
                graphics.drawString(this.font, "Always mode: " + tempAlwaysThreshold, leftX + 45, contentY + 5, 0xFFFFFFFF, false);
                graphics.drawString(this.font, "Night mode: " + tempNightThreshold, leftX + 45, contentY + 35, 0xFFFFFFFF, false);
                graphics.drawString(this.font, "Cave mode: " + tempCaveThreshold, leftX + 45, contentY + 65, 0xFFFFFFFF, false);
            }
            case LIGHT_SOURCES -> renderLightSourcesList(graphics, leftX, contentY + 30, mouseX, mouseY);
        }
    }

    private void renderLightSourcesList(GuiGraphics graphics, int x, int y, int mouseX, int mouseY) {
        graphics.drawString(this.font, "Priority List (drag to reorder):", x, y - 10, 0xFFFFFFFF, false);

        int displayCount = Math.min(ITEMS_PER_PAGE, tempLightSources.size() - scrollOffset);

        for (int i = 0; i < displayCount; i++) {
            int index = i + scrollOffset;
            String itemId = tempLightSources.get(index);
            int itemY = y + i * 25;

            // Фон элемента
            int bgColor = (mouseX >= x && mouseX <= x + 300 && mouseY >= itemY && mouseY <= itemY + 20)
                    ? 0x80333333 : 0x80222222;
            graphics.fill(x, itemY, x + 300, itemY + 20, bgColor);

            // Отрисовка иконки предмета
            try {
                ResourceLocation loc = ResourceLocation.parse(itemId);
                Item item = BuiltInRegistries.ITEM.getValue(loc);
                if (item != null && item != Items.AIR) {
                    graphics.renderItem(new ItemStack(item), x + 2, itemY + 2);
                }
            } catch (Exception ignored) {}

            graphics.drawString(this.font, itemId, x + 22, itemY + 6, 0xFFFFFFFF, false);

            if (index > 0) {
                graphics.drawString(this.font, "↑", x + 260, itemY + 6, 0xFF00FF00, false);
            }
            if (index < tempLightSources.size() - 1) {
                graphics.drawString(this.font, "↓", x + 275, itemY + 6, 0xFF00FF00, false);
            }
            graphics.drawString(this.font, "✕", x + 290, itemY + 6, 0xFFFF0000, false);
        }
    }

    private void renderSuggestions(GuiGraphics graphics, int mouseX, int mouseY) {
        int x = this.width / 2 - 150;
        int y = 110;

        for (int i = 0; i < Math.min(5, itemSuggestions.size()); i++) {
            int suggestionY = y + i * 20;
            int bgColor = (i == selectedSuggestion) ? 0x80101010 : 0x80dfdfdf;
            graphics.fill(x, suggestionY, x + 250, suggestionY + 14, bgColor);
            graphics.drawString(this.font, itemSuggestions.get(i), x + 2, suggestionY + 3, 0xFFFFFFFF, false);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent mbe, boolean bl) {
        double mouseX = mbe.x();
        double mouseY = mbe.y();

        if (currentTab == Tab.LIGHT_SOURCES) {
            int x = this.width / 2 - 150;
            int y = 110;
            int displayCount = Math.min(ITEMS_PER_PAGE, tempLightSources.size() - scrollOffset);

            for (int i = 0; i < displayCount; i++) {
                int index = i + scrollOffset;
                int itemY = y + i * 25;

                if (mouseX >= x && mouseX <= x + 300 && mouseY >= itemY && mouseY <= itemY + 20) {
                    if (mouseX >= x + 260 && mouseX <= x + 270 && index > 0) {
                        String temp = tempLightSources.get(index);
                        tempLightSources.set(index, tempLightSources.get(index - 1));
                        tempLightSources.set(index - 1, temp);
                        return true;
                    }
                    if (mouseX >= x + 275 && mouseX <= x + 285 && index < tempLightSources.size() - 1) {
                        String temp = tempLightSources.get(index);
                        tempLightSources.set(index, tempLightSources.get(index + 1));
                        tempLightSources.set(index + 1, temp);
                        return true;
                    }
                    if (mouseX >= x + 290 && mouseX <= x + 300) {
                        tempLightSources.remove(index);
                        if (scrollOffset > 0 && scrollOffset >= tempLightSources.size() - ITEMS_PER_PAGE) {
                            scrollOffset--;
                        }
                        rebuildWidgets();
                        return true;
                    }
                    if (mouseX < x + 260) {
                        this.isDragging = true;
                        this.draggingItemIndex = index;
                        return true;
                    }
                }
            }

            int suggestionY = 110;
            for (int i = 0; i < Math.min(5, itemSuggestions.size()); i++) {
                int sY = suggestionY + i * 20;
                if (mouseX >= x && mouseX <= x + 250 && mouseY >= sY && mouseY <= sY + 14) {
                    newItemInput.setValue(itemSuggestions.get(i));
                    itemSuggestions.clear();
                    return true;
                }
            }
        }
        return super.mouseClicked(mbe, bl);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent mbe) {
        if (mbe.button() == 0) {
            isDragging = false;
            draggingItemIndex = -1;
        }
        return super.mouseReleased(mbe);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent mbe, double d, double e) {
        double mouseY = mbe.y();
        if (isDragging && draggingItemIndex != -1 && currentTab == Tab.LIGHT_SOURCES) {
            int yStart = 110;
            int itemHeight = 25;
            int visualIndex = (int) ((mouseY - yStart) / itemHeight);
            int targetIndex = visualIndex + scrollOffset;

            if (targetIndex >= 0 && targetIndex < tempLightSources.size() && targetIndex != draggingItemIndex) {
                if (visualIndex >= 0 && visualIndex < ITEMS_PER_PAGE) {
                    String temp = tempLightSources.get(draggingItemIndex);
                    tempLightSources.set(draggingItemIndex, tempLightSources.get(targetIndex));
                    tempLightSources.set(targetIndex, temp);

                    draggingItemIndex = targetIndex;
                    return true;
                }
            }
        }
        return super.mouseDragged(mbe, d, e);
    }

    private void saveConfig() {
        ModConfig.prioritizedLightSources = new ArrayList<>(tempLightSources);
        ModConfig.forgetSettingsWhenLeavingWorld = tempForgetSettings;
        ModConfig.enableHotbarSwitch = tempEnableHotbarSwitch;
        ModConfig.placementCooldownTicks = tempPlacementCooldown;
        ModConfig.worksInWater = tempWorksInWater;
        ModConfig.HudSettings.hudX = tempHudX;
        ModConfig.HudSettings.hudY = tempHudY;
        ModConfig.ModeSettings.alwaysLightThreshold = tempAlwaysThreshold;
        ModConfig.ModeSettings.nightLightThreshold = tempNightThreshold;
        ModConfig.ModeSettings.caveLightThreshold = tempCaveThreshold;

        //TODO: doesn't work
        me.shedaniel.autoconfig.AutoConfig.getConfigHolder(ModConfig.class).save();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}