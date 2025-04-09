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
        return parent -> {
            if (ElytraMod.YACL_LOADED) {
                return YACL.getConfigScreen(parent);
            }
            return new Screen(Text.literal("Config Unavailable")) {
                @Override
                protected void init() {
                    this.addDrawableChild(ButtonWidget.builder(
                                    Text.literal("YACL Not Installed, Use JSON file"),
                                    button -> this.close()
                            )
                            .position(this.width / 2 - 100, this.height / 2)
                            .size(200, 20)
                            .build());
                }
            };
        };
    }
}