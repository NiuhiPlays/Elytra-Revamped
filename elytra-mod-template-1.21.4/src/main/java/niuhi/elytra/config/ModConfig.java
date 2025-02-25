package niuhi.elytra.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.*;
import java.nio.file.Path;

public class ModConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("elytra_mod_config.json");

    // Default configuration values
    public double baseBoost = 0.4;  // Default boost
    public double hayBoost = 0.8;   // Boost when above a haybale campfire
    public int normalFireHeight = 10; // Detection height for regular fire/campfires
    public int hayFireHeight = 25; // Detection height for haybale campfires
    public boolean disableFireworks = true; // Should fireworks be disabled?
    public double basePull = 0.1;  // Default boost
    public double hayPull = 0.25;   // Boost when above a haybale campfire

    // Load configuration from file
    public static ModConfig load() {
        if (CONFIG_PATH.toFile().exists()) {
            try (Reader reader = new FileReader(CONFIG_PATH.toFile())) {
                return GSON.fromJson(reader, ModConfig.class);
            } catch (IOException e) {
                System.err.println("Failed to load config file! Using default values.");
            }
        }
        return new ModConfig(); // Return default config if loading fails
    }

    // Save configuration to file
    public void save() {
        try (Writer writer = new FileWriter(CONFIG_PATH.toFile())) {
            GSON.toJson(this, writer);
        } catch (IOException e) {
            System.err.println("Failed to save config file!");
        }
    }
}