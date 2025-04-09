package niuhi.elytra.config;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import niuhi.elytra.ElytraMod;

public class ModMenu implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        DebugLogger.debug("YACL", "ModMenu requested config screen factory");

        return parent -> {
            if (ElytraMod.YACL_LOADED) {
                DebugLogger.debug("YACL", "YACL is loaded, returning YACL config screen");
                return YACL.getConfigScreen(parent);
            }

            DebugLogger.debug("YACL", "YACL not loaded, returning fallback screen");

            return new Screen(Text.literal("Config Unavailable")) {
                @Override
                protected void init() {
                    this.addDrawableChild(ButtonWidget.builder(
                                    Text.literal("YACL Not Installed, Use JSON file"),
                                    button -> {
                                        DebugLogger.debug("YACL", "Fallback screen closed by user");
                                        this.close();
                                    }
                            )
                            .position(this.width / 2 - 100, this.height / 2)
                            .size(200, 20)
                            .build());
                }
            };
        };
    }
}