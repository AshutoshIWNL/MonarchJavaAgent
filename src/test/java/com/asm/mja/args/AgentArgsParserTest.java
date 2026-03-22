package com.asm.mja.args;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit tests for agent argument parsing behavior.
 * @author ashut
 * @since 22-03-2026
 */
public class AgentArgsParserTest {

    @Test
    void parseReturnsNullFieldsWhenAgentArgsMissing() {
        AgentLaunchOptions options = AgentArgsParser.parse(null);
        assertNull(options.getConfigFile());
        assertNull(options.getAgentLogFileDir());
        assertNull(options.getAgentLogLevel());
        assertNull(options.getSmtpProperties());
        assertNull(options.getAgentJarPath());
    }

    @Test
    void parseReturnsAllFieldsWhenArgumentsAreValid() throws Exception {
        Path configFile = Files.createTempFile("mja-config", ".yaml");
        Path logDir = Files.createTempDirectory("mja-logdir");
        Path agentJar = Files.createTempFile("mja-agent", ".jar");

        String args =
                "configFile=" + configFile +
                        ",agentLogFileDir=" + logDir +
                        ",agentLogLevel=INFO" +
                        ",smtpProperties=C:\\smtp.properties" +
                        ",agentJarPath=" + agentJar;

        AgentLaunchOptions options = AgentArgsParser.parse(args);
        assertEquals(configFile.toString(), options.getConfigFile());
        assertEquals(logDir.toString(), options.getAgentLogFileDir());
        assertEquals("INFO", options.getAgentLogLevel());
        assertEquals("C:\\smtp.properties", options.getSmtpProperties());
        assertEquals(agentJar.toString(), options.getAgentJarPath());
    }

    @Test
    void parseReturnsAllFieldsWhenArgumentsAreValidTODO() throws Exception {
        Path configFile = Files.createTempFile("mja-config", ".yaml");
        Path logDir = Files.createTempDirectory("mja-logdir");
        Path agentJar = Files.createTempFile("mja-agent", ".jar");

        String args =
                "configFile=" + configFile +
                        ",agentLogFileDir=" + logDir +
                        ",agentLogLevel=INFO" +
                        ",smtpProperties=C:\\smtp.properties" +
                        ",agentJarPath=" + agentJar;

        AgentLaunchOptions options = AgentArgsParser.parse(args);
        assertEquals(configFile.toString(), options.getConfigFile());
        assertEquals(logDir.toString(), options.getAgentLogFileDir());
        assertEquals("INFO", options.getAgentLogLevel());
        assertEquals("C:\\smtp.properties", options.getSmtpProperties());
        assertEquals(agentJar.toString(), options.getAgentJarPath());
    }

    @Test
    void parseAgentLogLevelThrowsForUnknownLevel() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> AgentArgsParser.parseAgentLogLevel("agentLogLevel=NOT_A_LEVEL")
        );
        assertEquals("Invalid log level passed - NOT_A_LEVEL", ex.getMessage());
    }

    @Test
    void parseConfigFileThrowsWhenPathDoesNotExist() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> AgentArgsParser.parseConfigFile("configFile=C:\\does-not-exist\\m.yaml")
        );
        assertEquals("Config file doesn't exist in the specified directory - C:\\does-not-exist\\m.yaml", ex.getMessage());
    }
}
