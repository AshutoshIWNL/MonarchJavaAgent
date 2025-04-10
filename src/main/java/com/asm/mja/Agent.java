package com.asm.mja;

import java.io.File;
import java.lang.instrument.Instrumentation;
import java.util.jar.JarFile;

/**
 * Monarch's Entry Class
 *
 * @author ashut
 * @since 11-04-2024
 */
public class Agent {

    private final static String JAVA_AGENT_MODE = "javaagent";
    private final static String ATTACH_VM_MODE = "attachVM";

    /**
     * Entry point for premain. Sets up the logger and starts instrumenting.
     *
     * @param agentArgs  The agent arguments passed to the JVM.
     * @param inst       The instrumentation instance.
     */
    public static void premain(String agentArgs, Instrumentation inst) {
        AgentConfigurator.setupLogger(agentArgs);
        //With -Xbootclasspath, codeSource is null, so, will take the jar path from agent args
        AgentConfigurator.instrument(agentArgs, inst, JAVA_AGENT_MODE, null);
    }

    /**
     * Entry point for agentmain. Sets up the logger and starts instrumenting.
     *
     * @param agentArgs  The agent arguments passed to the JVM.
     * @param inst       The instrumentation instance.
     */
    public static void agentmain(String agentArgs, Instrumentation inst) {
        File agentJar = null;
        try {
            agentJar = new File(Agent.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            //Since we might be dealing with java.lang classes transformation, Boostrap classloader will be the one trying to load our agent classes downstream.
            //This will result in classloading issues due to visibility principle.
            //Normal approach is to use -Xbootclasspath but to avoid unnecessary JVM options setup, we are using the below to append our agent jar to bootstrap's classpath.
            inst.appendToBootstrapClassLoaderSearch(new JarFile(agentJar));
        } catch (Exception e) {
            System.err.println("Failure in agentmain: " + e.getMessage());
            return;
        }
        AgentConfigurator.setupLogger(agentArgs);
        AgentConfigurator.instrument(agentArgs, inst, ATTACH_VM_MODE, agentJar.getAbsolutePath());
    }

}
