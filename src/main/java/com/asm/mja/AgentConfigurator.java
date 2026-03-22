package com.asm.mja;

import com.asm.mja.bootstrap.AgentStartupOrchestrator;
import com.asm.mja.bootstrap.LoggingBootstrap;
import java.lang.instrument.Instrumentation;

/**
 * @author ashut
 * @since 23-04-2024
 */

public class AgentConfigurator {

    /**
     * Sets up the logger based on the provided agent arguments.
     *
     * @param agentArgs  The agent arguments passed to the JVM.
     */
    public static void setupLogger(String agentArgs) {
        LoggingBootstrap.setupLogger(agentArgs);
    }

    /**
     * Instruments the application with the specified configuration.
     *
     * @param agentArgs  The agent arguments passed to the JVM.
     * @param inst       The instrumentation instance.
     * @param launchType The type of launch: 'javaagent' or 'attachVM'.
     */
    public static void instrument(String agentArgs, Instrumentation inst, String launchType, String agentAbsolutePath) {
        AgentStartupOrchestrator.instrument(agentArgs, inst, launchType, agentAbsolutePath);
    }
}
