package com.fastbuildup.client;

import net.fabricmc.loader.api.FabricLoader;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public class ModConfig {
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("fastbuildup.properties");

    public boolean enabled = true;
    public int placementCooldownTicks = 1;
    public double verticalBoost = 0.42;
    public double maxVerticalSpeed = 0.55;
    public boolean sneakDisables = true;
    public boolean requireRightClick = true;
    public boolean requireJump = true;
    public boolean allowInWater = false;
    public boolean centerPlayerOnPillar = false;

    public void load() {
        if (!Files.exists(CONFIG_PATH)) {
            save();
            return;
        }

        Properties props = new Properties();
        try (InputStream in = Files.newInputStream(CONFIG_PATH)) {
            props.load(in);
            enabled = Boolean.parseBoolean(props.getProperty("enabled", "true"));
            placementCooldownTicks = Integer.parseInt(props.getProperty("placementCooldownTicks", "1"));
            verticalBoost = Double.parseDouble(props.getProperty("verticalBoost", "0.42"));
            maxVerticalSpeed = Double.parseDouble(props.getProperty("maxVerticalSpeed", "0.55"));
            sneakDisables = Boolean.parseBoolean(props.getProperty("sneakDisables", "true"));
            requireRightClick = Boolean.parseBoolean(props.getProperty("requireRightClick", "true"));
            requireJump = Boolean.parseBoolean(props.getProperty("requireJump", "true"));
            allowInWater = Boolean.parseBoolean(props.getProperty("allowInWater", "false"));
            centerPlayerOnPillar = Boolean.parseBoolean(props.getProperty("centerPlayerOnPillar", "false"));
        } catch (Exception e) {
            com.fastbuildup.FastBuildUpMod.LOGGER.error("Failed to load configuration, using defaults", e);
        }
    }

    public void save() {
        Properties props = new Properties();
        props.setProperty("enabled", String.valueOf(enabled));
        props.setProperty("placementCooldownTicks", String.valueOf(placementCooldownTicks));
        props.setProperty("verticalBoost", String.valueOf(verticalBoost));
        props.setProperty("maxVerticalSpeed", String.valueOf(maxVerticalSpeed));
        props.setProperty("sneakDisables", String.valueOf(sneakDisables));
        props.setProperty("requireRightClick", String.valueOf(requireRightClick));
        props.setProperty("requireJump", String.valueOf(requireJump));
        props.setProperty("allowInWater", String.valueOf(allowInWater));
        props.setProperty("centerPlayerOnPillar", String.valueOf(centerPlayerOnPillar));

        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            try (OutputStream out = Files.newOutputStream(CONFIG_PATH)) {
                props.store(out, "Fast Build Up Config");
            }
        } catch (Exception e) {
            com.fastbuildup.FastBuildUpMod.LOGGER.error("Failed to save configuration", e);
        }
    }
}
