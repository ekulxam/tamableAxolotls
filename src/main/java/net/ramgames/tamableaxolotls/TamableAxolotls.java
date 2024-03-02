package net.ramgames.tamableaxolotls;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TamableAxolotls implements ModInitializer {

    public static final String MOD_ID = "tamable_axolotls";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    @Override
    public void onInitialize() {
        LOGGER.info("tamable axolotls is booting up!");
    }
}
