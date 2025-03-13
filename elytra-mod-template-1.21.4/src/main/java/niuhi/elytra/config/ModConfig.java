package niuhi.elytra.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class ModConfig {
    // Fire boost configuration
    public FireBoostConfig fireBoost = new FireBoostConfig();
    // Soul fire configuration
    public SoulFireConfig soulFire = new SoulFireConfig();
    // General mechanics configuration
    public MechanicsConfig mechanics = new MechanicsConfig();

    public static class FireBoostConfig {
        public boolean enabled = true;
        public int detectionHeight = 10;
        public int hayDetectionHeight = 25;  // New: Increased detection range when hay bale is present
        public double baseBoost = 0.3;
        public double hayBoost = 0.5;
        public double distanceMultiplierClose = 0.8;  // 0-5 blocks
        public double distanceMultiplierMedium = 0.6; // 5-10 blocks
        public double distanceMultiplierFar = 0.4;    // 10+ blocks
        public int boostCooldownTicks = 0; // 0 = no cooldown
    }

    public static class SoulFireConfig {
        public boolean enabled = true;
        public int detectionHeight = 10;
        public int hayDetectionHeight = 25;  // New: Increased detection range when hay bale is present
        public double basePull = 0.3;
        public double hayPull = 0.5;
    }

    public static class MechanicsConfig {
        public boolean disableFireworks = true;
        public double minHorizontalVelocity = 0.1;
    }

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File CONFIG_FILE = FabricLoader.getInstance().getConfigDir().resolve("Elytra_Revamped.json").toFile();

    public static ModConfig load() {
        ModConfig config = new ModConfig();

        // Create default config if it doesn't exist
        if (!CONFIG_FILE.exists()) {
            save(config);
            return config;
        }

        // Load existing config
        try (FileReader reader = new FileReader(CONFIG_FILE)) {
            config = GSON.fromJson(reader, ModConfig.class);
        } catch (IOException e) {
            System.err.println("Error loading config: " + e.getMessage());
            save(config); // Save default config if loading fails
        }

        return config;
    }

    public static void save(ModConfig config) {
        try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
            GSON.toJson(config, writer);
        } catch (IOException e) {
            System.err.println("Error saving config: " + e.getMessage());
        }
    }
}