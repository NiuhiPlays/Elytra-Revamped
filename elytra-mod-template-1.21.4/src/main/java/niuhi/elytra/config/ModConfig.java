package niuhi.elytra.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import niuhi.elytra.ElytraMod;

import java.io.File;
import java.nio.file.Path;

public class ModConfig {
        public CampFireConfig campFire = new CampFireConfig();
        public SoulFireConfig soulFire = new SoulFireConfig();
        public MechanicsConfig mechanics = new MechanicsConfig();
        public FeedbackConfig feedback = new FeedbackConfig();
        public DragConfig drag = new DragConfig();

    public static class CampFireConfig {
        public boolean enabled = true;
        public int detectionHeight = 10;
        public int hayDetectionHeight = 25;
        public double baseBoost = 0.3;
        public double hayBoost = 0.5;
        public boolean autoScaleWithHeight = true;
        public int boostCooldownTicks = 0;
    }

    public static class SoulFireConfig {
        public boolean enabled = true;
        public int detectionHeight = 10;
        public int hayDetectionHeight = 25;
        public double basePull = 0.3;
        public double hayPull = 0.5;
        public boolean autoScaleWithHeight = true;
        public int pullCooldownTicks = 0;
    }

    public static class MechanicsConfig {
        public boolean disableFireworks = true;
        public boolean enableFireworkSmoke = true;
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
        public double dragFactor = 0.92;
        public boolean requireSneaking = true;
    }

    // Gson for manual serialization
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("Elytra_Revamped.json");
    private static ModConfig INSTANCE;

    // Initialize the config
    public static ModConfig init() {
        if (INSTANCE == null) {
            if (ElytraMod.YACL_LOADED) {
                // YACL will handle instantiation via YACLConfigScreen
                INSTANCE = loadManual(); // Load defaults or existing file initially
            }
        }
        return INSTANCE;
    }

    // Manual load method (used when YACL is absent or as initial load)
    private static ModConfig loadManual() {
        File configFile = CONFIG_PATH.toFile();
        if (configFile.exists()) {
            try (java.io.FileReader reader = new java.io.FileReader(configFile)) {
                return GSON.fromJson(reader, ModConfig.class);
            } catch (java.io.IOException e) {
                System.err.println("Error loading config: " + e.getMessage());
            }
        }
        ModConfig config = new ModConfig();
        saveManual(config);
        return config;
    }

    // Manual save method (used when YACL is absent)
    private static void saveManual(ModConfig config) {
        try (java.io.FileWriter writer = new java.io.FileWriter(CONFIG_PATH.toFile())) {
            GSON.toJson(config, writer);
        } catch (java.io.IOException e) {
            System.err.println("Error saving config: " + e.getMessage());
        }
    }

    // Public save method (uses YACL if available, otherwise manual)
    public static void save() {
        if (ElytraMod.YACL_LOADED) {
            // YACL handles saving via the screen
        } else {
            saveManual(INSTANCE);
        }
    }

    public static ModConfig getInstance() {
        return INSTANCE;
    }
}