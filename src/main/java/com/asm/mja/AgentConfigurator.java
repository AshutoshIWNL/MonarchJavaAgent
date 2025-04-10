package com.asm.mja;

import com.asm.mja.config.Config;
import com.asm.mja.config.ConfigParser;
import com.asm.mja.config.ConfigValidator;
import com.asm.mja.logging.AgentLogger;
import com.asm.mja.logging.LogLevel;
import com.asm.mja.logging.TraceFileLogger;
import com.asm.mja.monitor.JVMCPUMonitor;
import com.asm.mja.monitor.JVMMemoryMonitor;
import com.asm.mja.rule.Rule;
import com.asm.mja.rule.RuleParser;
import com.asm.mja.transformer.GlobalTransformer;
import com.asm.mja.utils.*;

import java.io.File;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author ashut
 * @since 23-04-2024
 */

public class AgentConfigurator {
    private final static String DEFAULT_LOG_LEVEL = "INFO";
    private final static String DEFAULT_AGENT_LOG_DIR = System.getProperty("java.io.tmpdir");
    private final static String AGENT_NAME = "Monarch";
    private final static String VERSION = "1.0";


    /**
     * Sets up the logger based on the provided agent arguments.
     *
     * @param agentArgs  The agent arguments passed to the JVM.
     */
    public static void setupLogger(String agentArgs) {
        String agentLogFileDir;
        try {
            agentLogFileDir = fetchAgentLogFileDir(agentArgs);
            if(agentLogFileDir == null)
                agentLogFileDir = DEFAULT_AGENT_LOG_DIR;
        } catch (IllegalArgumentException e) {
            System.err.println("ERROR: " + e.getMessage());
            agentLogFileDir = DEFAULT_AGENT_LOG_DIR;
        }

        String agentLogFile = agentLogFileDir + File.separator + "monarchAgent.log";

        String agentLogLevel;
        try {
            agentLogLevel = fetchAgentLogLevel(agentArgs);
            if(agentLogLevel == null)
                agentLogLevel = DEFAULT_LOG_LEVEL;
        } catch (IllegalArgumentException e) {
            System.err.println("ERROR: " + e.getMessage());
            agentLogLevel = DEFAULT_LOG_LEVEL;
        }
        AgentLogger.init(agentLogFile, LogLevel.valueOf(agentLogLevel));
    }

    /**
     * Fetches the agent log file directory from the agent arguments.
     *
     * @param agentArgs  The agent arguments passed to the JVM.
     * @return The agent log file directory.
     * @throws IllegalArgumentException If the agent log file directory is invalid.
     */
    private static String fetchAgentLogFileDir(String agentArgs) throws IllegalArgumentException {
        String agentLogFileDir = null;
        if (agentArgs != null) {
            String[] args = agentArgs.split(",");
            for (String arg : args) {
                if (arg.contains("agentLogFileDir")) {
                    String[] prop = arg.split("=");
                    if (prop.length < 2) {
                        throw new IllegalArgumentException("Invalid arguments passed - " + arg);
                    } else {
                        agentLogFileDir = prop[1];
                        File agentLogFileDirObj = new File(agentLogFileDir);
                        if (!agentLogFileDirObj.isDirectory() || !agentLogFileDirObj.exists()) {
                            throw new IllegalArgumentException("Agent logging doesn't exist or isn't a directory - " + agentLogFileDir);
                        }
                    }
                }
            }
        }
        return agentLogFileDir;
    }

    /**
     * Fetches the agent log level from the agent arguments.
     *
     * @param agentArgs  The agent arguments passed to the JVM.
     * @return The agent log level.
     * @throws IllegalArgumentException If the agent log level is invalid.
     */
    private static String fetchAgentLogLevel(String agentArgs) throws IllegalArgumentException {
        String agentLogLevel = null;
        if (agentArgs != null) {
            String[] args = agentArgs.split(",");
            for (String arg : args) {
                if (arg.contains("agentLogLevel")) {
                    String[] prop = arg.split("=");
                    if (prop.length < 2) {
                        throw new IllegalArgumentException("Invalid arguments passed - " + arg);
                    } else {
                        agentLogLevel = prop[1];
                        if(!isValidLogLevel(agentLogLevel)) {
                            throw new IllegalArgumentException("Invalid log level passed - " + agentLogLevel);
                        }
                    }
                }
            }
        }
        return agentLogLevel;
    }

    /**
     * Fetches the agent log level from the agent arguments.
     *
     * @param agentArgs  The agent arguments passed to the JVM.
     * @return The agent log level.
     * @throws IllegalArgumentException If the agent log level is invalid.
     */
    private static String fetchAgentJarPath(String agentArgs) throws IllegalArgumentException {
        String agentJarPath = null;
        if (agentArgs != null) {
            String[] args = agentArgs.split(",");
            for (String arg : args) {
                if (arg.contains("agentJarPath")) {
                    String[] prop = arg.split("=");
                    if (prop.length < 2) {
                        throw new IllegalArgumentException("Invalid arguments passed - " + arg);
                    } else {
                        agentJarPath = prop[1];
                    }
                }
            }
        }
        return agentJarPath;
    }

    private static String fetchSMTPProperties(String agentArgs) throws IllegalArgumentException {
        String smtpPropertiesFile = null;
        if (agentArgs != null) {
            String[] args = agentArgs.split(",");
            for (String arg : args) {
                if (arg.contains("smtpProperties")) {
                    String[] prop = arg.split("=");
                    if (prop.length < 2) {
                        throw new IllegalArgumentException("Invalid arguments passed - " + arg);
                    } else {
                        smtpPropertiesFile = prop[1];
                    }
                }
            }
        }
        return smtpPropertiesFile;
    }

    /**
     * Checks if the specified log level is valid.
     *
     * @param input The log level to check.
     * @return True if the log level is valid, otherwise false.
     */
    public static boolean isValidLogLevel(String input) {
        for (LogLevel level : LogLevel.values()) {
            if (level.name().equals(input)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Fetches the configuration file path from the agent arguments.
     *
     * @param agentArgs  The agent arguments passed to the JVM.
     * @return The configuration file path.
     * @throws IllegalArgumentException If the configuration file path is invalid.
     */
    public static String fetchConfigFile(String agentArgs) {
        AgentLogger.debug("Fetching config file to build the agent config");
        String configFile = null;
        if(agentArgs != null) {
            String[] args = agentArgs.split(",");

            for(String arg: args) {
                if(arg.contains("configFile")) {
                    String[] prop = arg.split("=");
                    if(prop.length < 2) {
                        throw new IllegalArgumentException("Invalid arguments passed - " + arg);
                    } else {
                        configFile = prop[1];
                        File configFileObj = new File(configFile);
                        if(!configFileObj.exists()) {
                            throw new IllegalArgumentException("Config file doesn't exist in the specified directory - " + configFile);
                        }
                    }
                }
            }
        }
        return configFile;
    }

    public static TraceFileLogger setupTraceFileLogger(String traceFileLocation) {
        TraceFileLogger traceFileLogger;
        String traceDir = traceFileLocation + File.separator + "Monarch_" + JVMUtils.getJVMPID() + "_" + DateUtils.getFormattedTimestampForFileName();
        File traceDirObj = new File(traceDir);
        if(traceDirObj.mkdir()) {
            traceFileLogger = TraceFileLogger.getInstance();
            traceFileLogger.init(traceDirObj.getAbsolutePath());
        }
        else {
            traceFileLogger = TraceFileLogger.getInstance();
            traceFileLogger.init(traceFileLocation);
        }
        return traceFileLogger;
    }

    public static void setupSMTP(String agentArgs, boolean isSendAlertEmail, List<String> emailRecipientList) {
        EmailUtils.init(fetchSMTPProperties(agentArgs), isSendAlertEmail, emailRecipientList);
    }

    /**
     * Instruments the application with the specified configuration.
     *
     * @param agentArgs  The agent arguments passed to the JVM.
     * @param inst       The instrumentation instance.
     * @param launchType The type of launch: 'javaagent' or 'attachVM'.
     */
    public static void instrument(String agentArgs, Instrumentation inst, String launchType, String agentAbsolutePath) {
        printStartup(agentArgs);

        if(agentAbsolutePath == null) {
            agentAbsolutePath = fetchAgentJarPath(agentArgs);
        }

        String configFile;
        try {
            configFile = AgentConfigurator.fetchConfigFile(agentArgs);
        } catch (IllegalArgumentException re) {
            AgentLogger.error("Exiting" + AGENT_NAME + " Java Agent due to exception - " + re.getMessage(),re);
            return;
        }

        Config config;
        try {
            config = ConfigParser.parse(configFile);
        } catch (RuntimeException re) {
            AgentLogger.error(String.format("Exiting %s Java Agent due to exception - %s", AGENT_NAME, re.getMessage()), re);
            return;
        }

        if(!ConfigValidator.isValid(config)) {
            AgentLogger.error("Config file isn't valid, exiting...");
            return;
        }

        if(!config.shouldInstrument()) {
            AgentLogger.warning("ShouldInstrument is set to false, exiting!");
            return;
        }

        HeapDumpUtils.setMaxHeapCount(config.getMaxHeapDumps());

        AgentLogger.debug("Creating TraceFileLogger instance for instrumentation logging");

        TraceFileLogger traceFileLogger = AgentConfigurator.setupTraceFileLogger(config.getTraceFileLocation());

        traceFileLogger.trace(AGENT_NAME + " Java Agent " + VERSION);
        traceFileLogger.trace(JVMUtils.getJVMCommandLine());

        AgentConfigurator.setupSMTP(agentArgs, config.isSendAlertEmails(), config.getEmailRecipientList());

        if(config.isPrintJVMSystemProperties()) {
            traceFileLogger.trace(JVMUtils.getJVMSystemProperties());
        }

        if(config.isPrintEnvironmentVariables()) {
            traceFileLogger.trace(JVMUtils.getEnvVars());
        }

        if(config.isPrintJVMHeapUsage()) {
            startJVMMemoryMonitorThread(traceFileLogger);
        }

        if(config.isPrintJVMCpuUsage()) {
            startJVMCpuMonitorThread(traceFileLogger);
        }

        List<String> rulesString = new ArrayList<>(config.getAgentRules());
        List<Rule> rules = RuleParser.parseRules(rulesString);
        GlobalTransformer globalTransformer = new GlobalTransformer(config, traceFileLogger, rules, launchType, agentAbsolutePath);

        AgentLogger.debug("Launch Type \"" + launchType + "\" detected");

        boolean retransformSupported = inst.isRetransformClassesSupported();
        if (retransformSupported) {
            Class<?>[] classesToInstrument = ClassRuleUtils.ruleClasses(inst.getAllLoadedClasses(), rules);
            inst.addTransformer(globalTransformer, Boolean.TRUE);

            try {
                AgentLogger.debug("Re-transforming classes: " + Arrays.toString(classesToInstrument));
                inst.retransformClasses(classesToInstrument);
            } catch (UnmodifiableClassException e) {
                AgentLogger.error("Error re-transforming classes: " + e.getMessage(), e);
            }
        } else {
            AgentLogger.debug("Retransform not supported, adding transformer for future class loads.");
            inst.addTransformer(globalTransformer, Boolean.FALSE);
        }
        AgentLogger.info("Registered transformer - " + GlobalTransformer.class);

        startInstrumentationManager(inst, configFile, globalTransformer, traceFileLogger, rules, config.getConfigRefreshInterval());

        AgentLogger.debug("Setting up shutdown hook to close resources");
        Thread shutdownHook = new Thread(() -> {
            JVMMemoryMonitor jvmMemoryMonitor = JVMMemoryMonitor.getInstance();
            if(!jvmMemoryMonitor.isDown())
                JVMMemoryMonitor.getInstance().shutdown();
            JVMCPUMonitor jvmcpuMonitor = JVMCPUMonitor.getInstance();
            if(!jvmcpuMonitor.isDown())
                JVMCPUMonitor.getInstance().shutdown();
            traceFileLogger.close();
        });
        shutdownHook.setName("monarch-shutdown-hook");
        Runtime.getRuntime().addShutdownHook(shutdownHook);


        AgentLogger.deinit();
    }

    /**
     * Starts the JVM CPU Monitor thread
     *
     * @param traceFileLogger  The logger to be used by JVM Monitor
     */
    private static void startJVMCpuMonitorThread(TraceFileLogger traceFileLogger) {
        JVMCPUMonitor jvmcpuMonitor = JVMCPUMonitor.getInstance();
        jvmcpuMonitor.setLogger(traceFileLogger);
        jvmcpuMonitor.execute();
    }

    /**
     * Starts the JVM Memory Monitor thread
     *
     * @param traceFileLogger  The logger to be used by JVM Monitor
     */
    private static void startJVMMemoryMonitorThread(TraceFileLogger traceFileLogger) {
        JVMMemoryMonitor jvmMemoryMonitor = JVMMemoryMonitor.getInstance();
        jvmMemoryMonitor.setLogger(traceFileLogger);
        jvmMemoryMonitor.execute();
    }

    /**
     * Initializes and starts the Instrumentation Manager with the provided parameters.
     *
     * @param inst                The Instrumentation instance used to perform bytecode manipulation.
     * @param configFile          The path to the configuration file for the Instrumentation Manager.
     * @param globalTransformer   The GlobalTransformer instance that will manage bytecode transformations.
     * @param traceFileLogger     The logger responsible for tracing file operations and instrumentation logs.
     * @param rules             The list of rules to apply during instrumentation.
     * @param configRefreshInterval The interval (in milliseconds) at which the configuration file is checked for updates.
     */
    private static void startInstrumentationManager(Instrumentation inst, String configFile, GlobalTransformer globalTransformer,
                                                    TraceFileLogger traceFileLogger, List<Rule> rules, long configRefreshInterval) {
        InstrumentationManager instrumentationManager = InstrumentationManager.getInstance();
        instrumentationManager.setInstrumentation(inst);
        instrumentationManager.setConfigFilePath(configFile);
        instrumentationManager.setJvmMemoryMonitor(JVMMemoryMonitor.getInstance());
        instrumentationManager.setTransformer(globalTransformer);
        instrumentationManager.setCurrentRules(rules);
        instrumentationManager.setLastModified(new File(configFile).lastModified());
        instrumentationManager.setLogger(traceFileLogger);
        instrumentationManager.setConfigRefreshInterval(configRefreshInterval);
        instrumentationManager.execute();
    }


    /**
     * Prints the startup information.
     *
     * @param agentArgs The agent arguments passed to the JVM.
     */
    private static void printStartup(String agentArgs) {
        AgentLogger.draw(BannerUtils.getBanner(AGENT_NAME + " JAVA AGENT"));
        AgentLogger.info("Starting " + AGENT_NAME + " " + VERSION + " @ " + DateUtils.getFormattedTimestamp());
        AgentLogger.info("Agent arguments - " + agentArgs);
    }
}
