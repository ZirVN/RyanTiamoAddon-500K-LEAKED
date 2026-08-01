package com.zirvn.addon.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

public class ModConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File CONFIG_FILE = new File(FabricLoader.getInstance().getConfigDir().toFile(), "ziraddon-addon.json");
    private static ModConfig instance = new ModConfig();

    @SerializedName("modEnabled")
    public boolean modEnabled = true;
    @SerializedName("autoSneak")
    public boolean autoSneak = true;
    @SerializedName("placeDelayMs")
    public int placeDelayMs = 110;
    @SerializedName("pauseMovementDuringDelay")
    public boolean pauseMovementDuringDelay = true;
    @SerializedName("autoAdjustRedstone")
    public boolean autoAdjustRedstone = true;
    @SerializedName("autoPickFromInventory")
    public boolean autoPickFromInventory = true;
    @SerializedName("limitToSchematic")
    public boolean limitToSchematic = true;
    @SerializedName("preventContainerInteraction")
    public boolean preventContainerInteraction = true;
    @SerializedName("forcedRotationState")
    public RotationState forcedRotationState;
    @SerializedName("targetItems")
    public List<String> targetItems;

    public ModConfig() {
        this.forcedRotationState = RotationState.AUTO;
        this.targetItems = new ArrayList<>();
    }

    public boolean isTargetItem(ItemStack stack) {
        if (this.targetItems.isEmpty()) {
            return true;
        } else {
            Identifier id = Registries.ITEM.getId(stack.getItem());
            return id != null && this.targetItems.contains(id.toString());
        }
    }

    public static ModConfig get() {
        return instance;
    }

    public static void load() {
        if (CONFIG_FILE.exists()) {
            try (FileReader reader = new FileReader(CONFIG_FILE)) {
                instance = GSON.fromJson(reader, ModConfig.class);
                if (instance == null) {
                    instance = new ModConfig();
                }
            } catch (Exception e) {
                System.out.println("[ZirAddon] Config corrupted, resetting to defaults: " + e.getMessage());
                instance = new ModConfig();
                CONFIG_FILE.delete();
            }
        } else {
            save();
        }
    }

    public static void save() {
        try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
            GSON.toJson(instance, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public enum RotationState {
        AUTO,
        UP,
        DOWN,
        NORTH,
        SOUTH,
        WEST,
        EAST
    }
}

