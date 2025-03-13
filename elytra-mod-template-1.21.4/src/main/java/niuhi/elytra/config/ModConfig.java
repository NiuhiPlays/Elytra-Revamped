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
    // Feedback configuration
    public FeedbackConfig feedback = new FeedbackConfig();
    // Drag configuration
    public DragConfig drag = new DragConfig();

    public static class FireBoostConfig {
        public boolean enabled = true;
        public int detectionHeight = 10;
        public int hayDetectionHeight = 25;  // Increased detection range when hay bale is present
        public double baseBoost = 0.3;
        public double hayBoost = 0.5;
        public boolean autoScaleWithHeight = true;  // Option to enable/disable automatic height scaling
        public int boostCooldownTicks = 0; // 0 = no cooldown  - 20 = 1 Sec
    }

    public static class SoulFireConfig {
        public boolean enabled = true;
        public int detectionHeight = 10;
        public int hayDetectionHeight = 25;  // Increased detection range when hay bale is present
        public double basePull = 0.3;
        public double hayPull = 0.5;
        public boolean autoScaleWithHeight = true;  // Option to enable/disable automatic height scaling
        public int pullCooldownTicks = 0; // 0 = no cooldown - 20 = 1 Sec
    }

    public static class MechanicsConfig {
        public boolean disableFireworks = true;
        public boolean enableFireworkSmoke = true; // New option for firework smoke effect
        public double minHorizontalVelocity = 0.1;
    }

    public static class FeedbackConfig {
        public boolean enableParticles = true;
        public boolean enableSounds = true;
        public float soundVolume = 0.5f;
        public float soundPitch = 1.0f;
    }

    public static class DragConfig {
        public boolean enabled = true;
        public double dragFactor = 0.92; // Values closer to 0 = more drag, 1.0 = no drag
        public boolean requireSneaking = true;
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