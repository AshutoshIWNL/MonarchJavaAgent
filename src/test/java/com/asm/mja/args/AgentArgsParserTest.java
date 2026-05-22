package com.asm.mja.args;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit tests for agent argument parsing behavior.
 *
 * @author anandh
 * @since 22-05-2026
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
    void parseIgnoresUnknownKeys() throws Exception {

        Path configFile = Files.createTempFile("mja-config", ".yaml");

        String args =
                "configFile=" + configFile +
                        ",unknownKey=test";

        AgentLaunchOptions options = AgentArgsParser.parse(args);

        assertEquals(configFile.toString(), options.getConfigFile());
        assertNull(options.getAgentLogFileDir());
        assertNull(options.getAgentLogLevel());
    }

    @Test
    void parseIgnoresMalformedTokens() throws Exception {

        Path configFile = Files.createTempFile("mja-config", ".yaml");

        String args =
                "configFile=" + configFile +
                        ",badtoken" +
                        ",anotherbadtoken";

        AgentLaunchOptions options = AgentArgsParser.parse(args);

        assertEquals(configFile.toString(), options.getConfigFile());
    }

    @Test
    void parseThrowsForDuplicateKeys() {

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                new Executable() {
                    @Override
                    public void execute() {
                        AgentArgsParser.validateAgentLogFileDir(
                                "C:\\does-not-exist\\logs"
                        );
                    }
                }
        );

        assertEquals(
                "Agent logging directory doesn't exist or isn't a directory: C:\\does-not-exist\\logs",
                ex.getMessage()
        );
    }

    @Test
    void validateAgentLogLevelThrowsForUnknownLevel() {

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                new Executable() {
                    @Override
                    public void execute() {
                        AgentArgsParser.validateAgentLogLevel("NOT_A_LEVEL");
                    }
                }
        );

        assertEquals(
                "Invalid log level passed: NOT_A_LEVEL",
                ex.getMessage()
        );
    }

    @Test
    void validateConfigFileThrowsWhenPathDoesNotExist() {

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                new Executable() {
                    @Override
                    public void execute() {
                        AgentArgsParser.validateConfigFile(
                                "C:\\does-not-exist\\m.yaml"
                        );
                    }
                }
        );

        assertEquals(
                "Config file doesn't exist or is invalid: C:\\does-not-exist\\m.yaml",
                ex.getMessage()
        );
    }

    @Test
    void validateAgentJarPathThrowsWhenJarDoesNotExist() {

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                new Executable() {
                    @Override
                    public void execute() {
                        AgentArgsParser.validateAgentJarPath(
                                "C:\\does-not-exist\\agent.jar"
                        );
                    }
                }
        );

        assertEquals(
                "Agent jar doesn't exist or is invalid: C:\\does-not-exist\\agent.jar",
                ex.getMessage()
        );
    }

    @Test
    void validateAgentLogFileDirThrowsWhenDirectoryDoesNotExist() {

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                new Executable() {
                    @Override
                    public void execute() {
                        AgentArgsParser.validateAgentLogFileDir(
                                "C:\\does-not-exist\\logs"
                        );
                    }
                }
        );

        assertEquals(
                "Agent logging directory doesn't exist or isn't a directory: C:\\does-not-exist\\logs",
                ex.getMessage()
        );
    }

    @Test
    void isValidLogLevelReturnsTrueForValidLevels() {

        assertEquals(true, AgentArgsParser.isValidLogLevel("INFO"));
        assertEquals(true, AgentArgsParser.isValidLogLevel("DEBUG"));
        assertEquals(true, AgentArgsParser.isValidLogLevel("ERROR"));
    }

    @Test
    void isValidLogLevelReturnsFalseForInvalidLevels() {

        assertEquals(false, AgentArgsParser.isValidLogLevel("INVALID"));
        assertEquals(false, AgentArgsParser.isValidLogLevel(""));
        assertEquals(false, AgentArgsParser.isValidLogLevel(null));
    }
}