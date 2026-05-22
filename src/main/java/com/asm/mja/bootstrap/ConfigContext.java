package com.asm.mja.bootstrap;

import com.asm.mja.config.Config;
import com.asm.mja.config.ConfigValidationResult;

/**
 * Holds resolved configuration file path and parsed config object.
 * @author ashut
 * @since 22-03-2026
 */
public class ConfigContext {
    private final String configFile;
    private final Config config;
    private final ConfigValidationResult validationResult;

    public ConfigContext(String configFile, Config config, ConfigValidationResult validationResult) {
        this.configFile = configFile;
        this.config = config;
        this.validationResult = validationResult;
    }

    public String getConfigFile() {
        return configFile;
    }

    public Config getConfig() {
        return config;
    }

    public ConfigValidationResult getValidationResult() {
        return validationResult;
    }
}
