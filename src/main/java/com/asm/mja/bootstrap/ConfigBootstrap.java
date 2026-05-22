package com.asm.mja.bootstrap;

import com.asm.mja.config.Config;
import com.asm.mja.config.ConfigParser;
import com.asm.mja.config.ConfigValidationResult;
import com.asm.mja.config.ConfigValidator;
import com.asm.mja.logging.AgentLogger;

import java.io.File;

/**
 * Resolves and parses runtime agent configuration.
 * @author ashut
 * @since 22-03-2026
 */
public class ConfigBootstrap {

    private ConfigBootstrap() {
    }

    public static ConfigContext load(String agentArgs) {
        String configFile = fetchConfigFile(agentArgs);
        Config config = ConfigParser.parse(configFile);
        ConfigValidationResult validationResult = ConfigValidator.validate(config);
        return new ConfigContext(configFile, config, validationResult);
    }

    public static boolean isValid(ConfigValidationResult validationResult) {
        return validationResult != null && validationResult.isValid();
    }

    static String fetchConfigFile(String agentArgs) {
        AgentLogger.debug("Fetching config file to build the agent config");
        String configFile = null;
        if (agentArgs != null) {
            String[] args = agentArgs.split(",");

            for (String arg : args) {
                if (arg.contains("configFile")) {
                    String[] prop = arg.split("=");
                    if (prop.length < 2) {
                        throw new IllegalArgumentException("Invalid arguments passed - " + arg);
                    } else {
                        configFile = prop[1];
                        File configFileObj = new File(configFile);
                        if (!configFileObj.exists()) {
                            throw new IllegalArgumentException("Config file doesn't exist in the specified directory - " + configFile);
                        }
                    }
                }
            }
        }
        return configFile;
    }
}
