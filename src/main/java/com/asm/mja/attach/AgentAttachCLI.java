package com.asm.mja.attach;

import com.sun.tools.attach.VirtualMachine;
import org.apache.commons.cli.*;

import java.io.File;

/**
 * A command-line interface (CLI) tool for attaching a Java agent to a running JVM.
 *
 * This class provides a way to attach an agent JAR file to a target Java process
 * identified by its PID. It uses command-line options to specify the agent JAR path,
 * configuration file, additional arguments, and the target JVM PID.
 *
 * @author ashut
 * @since 05-05-2024
 */
public class AgentAttachCLI {
    public static void main(String[] args) {
        Options options = new Options();
        options.addOption("agentJar", true, "Path to the agent JAR file");
        options.addOption("configFile", true, "Path to the agent config file");
        options.addOption("args", true, "Arguments to pass to the agent");
        options.addOption("pid", true, "PID of the target JVM");

        CommandLineParser parser = new DefaultParser();
        try {
            CommandLine cmd = parser.parse(options, args);

            String agentFilePath = cmd.getOptionValue("agentJar");
            String configFile = cmd.getOptionValue("configFile");
            String agentArgs = cmd.getOptionValue("args");
            String targetPID = cmd.getOptionValue("pid");
            attachAgent(agentFilePath, configFile, agentArgs, targetPID);
        } catch (ParseException e) {
            System.err.println("Error parsing command-line arguments: " + e.getMessage());
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("java -jar AgentAttacherCLI.jar", options);
        } catch (Exception e) {
            System.err.println("Error while attaching agent to the target JVM" + e.getMessage());
        }
    }

    /**
     * Attaches the specified agent to a target JVM identified by its PID.
     *
     * @param agentFilePath The file path to the agent JAR.
     * @param configFile    The path to the configuration file for the agent.
     * @param agentArgs     Additional arguments to pass to the agent.
     * @param targetPID     The PID of the target JVM to which the agent will be attached.
     * @throws Exception If there is an error attaching the agent to the target JVM.
     */
    private static void attachAgent(String agentFilePath, String configFile, String agentArgs, String targetPID) throws Exception {
        VirtualMachine vm = VirtualMachine.attach(targetPID);
        String args = "configFile=" + configFile + "," + agentArgs;
        vm.loadAgent(new File(agentFilePath).getAbsolutePath(), args);
        vm.detach();
        System.out.println("Agent attached successfully to PID " + targetPID);
    }

}
