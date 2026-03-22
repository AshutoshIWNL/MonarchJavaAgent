package com.asm.mja.bootstrap;

import com.asm.mja.config.Config;
import com.asm.mja.config.ConfigParser;
import com.asm.mja.logging.AgentLogger;
import com.asm.mja.logging.TraceFileLogger;
import com.asm.mja.rule.Rule;
import com.asm.mja.rule.RuleParser;
import com.asm.mja.transformer.GlobalTransformer;
import com.asm.mja.utils.BannerUtils;
import com.asm.mja.utils.DateUtils;
import com.asm.mja.utils.HeapDumpUtils;
import com.asm.mja.utils.JVMUtils;

import java.io.File;
import java.lang.instrument.Instrumentation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Orchestrates agent startup and instrumentation sequence.
 * @author ashut
 * @since 22-03-2026
 */
public class AgentStartupOrchestrator {
    private static final String AGENT_NAME = "Monarch";
    private static final String VERSION = "1.1";

    private AgentStartupOrchestrator() {
    }

    public static void instrument(String agentArgs, Instrumentation inst, String launchType, String agentAbsolutePath) {
        printStartup(agentArgs);

        if (agentAbsolutePath == null) {
            agentAbsolutePath = fetchAgentJarPath(agentArgs);
        }

        String configFile;
        Config config;
        try {
            ConfigContext configContext = ConfigBootstrap.load(agentArgs);
            configFile = configContext.getConfigFile();
            config = configContext.getConfig();
        } catch (RuntimeException re) {
            AgentLogger.error(String.format("Exiting %s Java Agent due to exception - %s", AGENT_NAME, re.getMessage()), re);
            return;
        }

        if (!ConfigBootstrap.isValid(config)) {
            AgentLogger.error("Config file isn't valid, exiting...");
            return;
        }

        boolean instrumentationActive = config.isInstrumentationActive();
        boolean observerActive = config.isObserverActive();
        boolean alertsActive = config.isAlertsActive();

        if (!instrumentationActive && !observerActive) {
            AgentLogger.warning("No active agent components configured for mode " + config.getResolvedMode() + ", exiting.");
            return;
        }

        if (config.getTraceFileLocation() == null || config.getTraceFileLocation().isEmpty()) {
            AgentLogger.error("Trace file location is required when instrumentation or observer components are active");
            return;
        }

        AgentLogger.debug("Creating TraceFileLogger instance for instrumentation logging");
        TraceFileLogger traceFileLogger = TraceBootstrap.setupTraceFileLogger(config.getTraceFileLocation());

        traceFileLogger.trace(AGENT_NAME + " Java Agent " + VERSION);
        traceFileLogger.trace(JVMUtils.getJVMCommandLine());

        if (alertsActive) {
            HeapDumpUtils.setMaxHeapCount(config.getMaxHeapDumps());
            SmtpBootstrap.setupSMTP(agentArgs, config.isSendAlertEmails(), config.getEmailRecipientList());
        }

        if (observerActive && config.isPrintJVMSystemProperties()) {
            traceFileLogger.trace(JVMUtils.getJVMSystemProperties());
        }

        if (observerActive && config.isPrintEnvironmentVariables()) {
            traceFileLogger.trace(JVMUtils.getEnvVars());
        }

        List<Rule> rules = Collections.emptyList();
        GlobalTransformer globalTransformer = null;
        if (instrumentationActive) {
            List<String> rulesString = new ArrayList<>(config.getAgentRules());
            rules = RuleParser.parseRules(rulesString);
            globalTransformer = TransformerBootstrap.setupTransformer(
                    inst,
                    config,
                    traceFileLogger,
                    rules,
                    launchType,
                    agentAbsolutePath
            );
        } else {
            traceFileLogger.trace("Instrumentation startup skipped for mode " + config.getResolvedMode());
        }

        InstrumentationManagerBootstrap.start(inst, configFile, globalTransformer, traceFileLogger, rules, config);

        AgentLogger.debug("Setting up shutdown hook to close resources");
        Thread shutdownHook = new Thread(traceFileLogger::close);
        shutdownHook.setName("monarch-shutdown-hook");
        Runtime.getRuntime().addShutdownHook(shutdownHook);

        AgentLogger.deinit();
    }

    private static String fetchAgentJarPath(String agentArgs) {
        String jarPath = null;
        if (agentArgs != null) {
            String[] args = agentArgs.split(",");
            for (String arg : args) {
                if (arg.contains("agentJarPath")) {
                    String[] prop = arg.split("=");
                    if (prop.length < 2) {
                        throw new IllegalArgumentException("Invalid arguments passed - " + arg);
                    } else {
                        jarPath = prop[1];
                        File agentJarPathObj = new File(jarPath);
                        if (agentJarPathObj.isDirectory() || !agentJarPathObj.exists()) {
                            throw new IllegalArgumentException("Agent jar doesn't exist or is a directory - " + jarPath);
                        }
                    }
                }
            }
        }
        return jarPath;
    }

    private static void printStartup(String agentArgs) {
        AgentLogger.draw(BannerUtils.getBanner(AGENT_NAME + " JAVA AGENT"));
        AgentLogger.info("Starting " + AGENT_NAME + " " + VERSION + " @ " + DateUtils.getFormattedTimestamp());
        AgentLogger.info("Agent arguments - " + agentArgs);
    }
}
