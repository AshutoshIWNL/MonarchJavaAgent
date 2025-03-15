package com.asm.mja.config;

import com.asm.mja.AgentConfigurator;
import com.asm.mja.logging.AgentLogger;
import com.asm.mja.logging.TraceFileLogger;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.File;
import java.io.IOException;

/**
 * The ConfigParser class parses a configuration file into a Config object.
 * It uses Jackson ObjectMapper for parsing YAML files into Java objects.
 *
 * This class provides a static method parse() to parse the configuration file.
 *
 * @author ashut
 * @since 11-04-2024
 */

public class ConfigParser {

    /**
     * Parses the given configuration file into a Config object.
     *
     * @param configFile The path to the configuration file.
     * @return The Config object parsed from the configuration file.
     * @throws RuntimeException if parsing fails due to IO error or invalid YAML format.
     */
    public static Config parse(String configFile) {
        AgentLogger.debug("Parsing config file - " + configFile);
        Config config = null;
        ObjectMapper om = new ObjectMapper(new YAMLFactory());
        try {
            File configFileObj = new File(configFile);
            config = om.readValue(configFileObj, Config.class);
        } catch (IOException e) {
            AgentLogger.error(e.getMessage(), e);
            throw new RuntimeException("Config file parsing failed");
        }
        AgentLogger.debug("Config file parsed and config object built");
        AgentLogger.debug(config.toString());
        return config;
    }

    public static Config parse(String configFile, TraceFileLogger logger) throws IOException {
        logger.trace("Parsing config file - " + configFile);
        ObjectMapper om = new ObjectMapper(new YAMLFactory());
        File configFileObj = new File(configFile);
        Config config = om.readValue(configFileObj, Config.class);
        logger.trace("Config file parsed and config object built");
        logger.trace(config.toString());
        return config;
    }
}
