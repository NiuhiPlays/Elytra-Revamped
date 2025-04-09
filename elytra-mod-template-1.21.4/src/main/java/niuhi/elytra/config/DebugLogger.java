package niuhi.elytra.config;

import niuhi.elytra.ElytraMod;
import org.slf4j.Logger;

import java.text.SimpleDateFormat;
import java.util.Date;

public class DebugLogger {
    private static final Logger LOGGER = ElytraMod.LOGGER;
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public static void debug(String handler, String message, Object... args) {
        ModConfig config = ModConfig.getInstance();
        if (config == null) {
            LOGGER.error("DebugLogger: Config is null, cannot proceed with logging");
            return;
        }

        // Check for invalid handler
        if (handler == null || handler.trim().isEmpty()) {
            LOGGER.warn("DebugLogger: Invalid handler (null or empty) called with message: {}", String.format(message, args));
            return;
        }

        boolean debugEnabled = config.debug.enabled;
        boolean handlerEnabled = isHandlerEnabled(handler, config);
        boolean shouldLog = debugEnabled && handlerEnabled;

        if (shouldLog) {
            String formattedMessage = String.format(message, args);
            String timestampedMessage = String.format("[ElytraRevamped-Debug] [%s] [%s] %s",
                    DATE_FORMAT.format(new Date()), handler, formattedMessage);
            LOGGER.info(timestampedMessage);
        }
    }

    private static boolean isHandlerEnabled(String handler, ModConfig config) {
        boolean enabled = switch (handler) {
            case "FireBoostHandler" -> config.debug.fireBoostHandler;
            case "SoulFireHandler" -> config.debug.soulFireHandler;
            case "FireworkSmokeHandler" -> config.debug.fireworkSmokeHandler;
            case "DragHandler" -> config.debug.dragHandler;
            case "InitialFlightHandler" -> config.debug.initialFlightHandler;
            case "FeedbackHandler" -> config.debug.feedbackHandler;
            case "FlightDetector" -> config.debug.flightDetector;
            case "Accessories" -> config.debug.Accessories;
            case "YACL" -> config.debug.YACL;
            default -> {
                LOGGER.warn("DebugLogger: Unknown debug handler: '{}'", handler);
                yield false;
            }
        };
        return enabled;
    }
}