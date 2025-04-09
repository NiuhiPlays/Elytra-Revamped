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
    public DebugMode debug = new DebugMode();

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
        public boolean allowInitialFirework = false;
        public int initialFireworkGraceTicks = 20;
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
    }

    public static class DebugMode {
        public boolean enabled = false;
        public boolean fireBoostHandler = true;
        public boolean soulFireHandler = true;
        public boolean fireworkSmokeHandler = true;
        public boolean dragHandler = true;
        public boolean initialFlightHandler = true;
        public boolean feedbackHandler = true;
        public boolean flightDetector = true;
        public boolean Accessories = true;
        public boolean YACL = true;
    }

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("Elytra_Revamped.json");
    private static ModConfig INSTANCE;

    public static ModConfig init() {
        if (INSTANCE == null) {
            INSTANCE = loadManual();
        }
        return INSTANCE;
    }

    private static ModConfig loadManual() {
        File configFile = CONFIG_PATH.toFile();
        if (configFile.exists()) {
            try (java.io.FileReader reader = new java.io.FileReader(configFile)) {
                ModConfig loaded = GSON.fromJson(reader, ModConfig.class);
                if (loaded != null) {
                    return loaded;
                }
            } catch (java.io.IOException e) {
                System.err.println("Error loading config: " + e.getMessage());
            }
        }
        ModConfig config = new ModConfig();
        saveManual(config);
        return config;
    }

    private static void saveManual(ModConfig config) {
        try (java.io.FileWriter writer = new java.io.FileWriter(CONFIG_PATH.toFile())) {
            GSON.toJson(config, writer);
        } catch (java.io.IOException e) {
            System.err.println("Error saving config: " + e.getMessage());
        }
    }

    public static void save() {
        if (ElytraMod.YACL_LOADED) {
            // YACL handles saving via the screen
        } else {
            saveManual(INSTANCE);
        }
    }

    public static ModConfig getInstance() {
        if (INSTANCE == null) {
            INSTANCE = loadManual(); // Ensure instance is always available
        }
        return INSTANCE;
    }
}