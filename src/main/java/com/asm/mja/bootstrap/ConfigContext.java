package com.asm.mja.bootstrap;

import com.asm.mja.config.Config;

/**
 * Holds resolved configuration file path and parsed config object.
 * @author ashut
 * @since 22-03-2026
 */
public class ConfigContext {
    private final String configFile;
    private final Config config;

    public ConfigContext(String configFile, Config config) {
        this.configFile = configFile;
        this.config = config;
    }

    public String getConfigFile() {
        return configFile;
    }

    public Config getConfig() {
        return config;
    }
}
