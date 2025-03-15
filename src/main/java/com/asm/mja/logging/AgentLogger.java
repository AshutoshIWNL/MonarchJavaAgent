package com.asm.mja.logging;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * The AgentLogger class provides logging functionality for the agent.
 * @author ashut
 * @since 11-04-2024
 */

public class AgentLogger {

    private static PrintWriter writer;
    private static LogLevel globalLogLevel;

    /**
     * Initializes the logger with the specified log file path and log level.
     *
     * @param logFilePath The path to the log file.
     * @param logLevel    The global log level.
     */
    public static void init(String logFilePath, LogLevel logLevel) {
        globalLogLevel = logLevel;
        try {
            writer = new PrintWriter(new FileWriter(logFilePath, true));
        } catch (IOException e) {
            System.err.println("Error opening log file: " + e.getMessage());
        }
    }

    /**
     * Logs a message with the specified log level.
     *
     * @param message  The message to log.
     * @param logLevel The log level of the message.
     */
    public static void log(String message, LogLevel logLevel) {
        if (logLevel.ordinal() < globalLogLevel.ordinal()) {
            return; // Skip logging if log level is lower than global log level
        }
        if (writer == null) {
            System.err.println("Error: Log file writer is not initialized. Call init() method first.");
            return;
        }
        writer.println("[" + logLevel.name() + "] " + message);
        writer.flush(); // Ensure the message is written immediately
    }

    /**
     * For dumping unformatted text to the log file
     * Used to draw the banner & to dump exceptions in the log file
     * @param message message to be dumped.
     */
    public static void draw(String message) {
        if (writer == null) {
            System.err.println("Error: Log file writer is not initialized. Call init() method first.");
            return;
        }
        writer.println(message);
        writer.flush(); // Ensure the message is written immediately
    }

    public static void trace(String message) {
        log(message, LogLevel.DEBUG);
    }
    public static void debug(String message) {
        log(message, LogLevel.DEBUG);
    }
    public static void info(String message) {
        log(message, LogLevel.INFO);
    }
    public static void warning(String message) {
        log(message, LogLevel.WARNING);
    }
    public static void error(String message) {
        log(message, LogLevel.ERROR);
    }
    public static void error(String message, Exception e) {
        log(message, LogLevel.ERROR);
        dumpException(e);
    }

    /**
     * Closes the log file writer.
     */
    public static void close() {
        if (writer != null) {
            writer.close();
        }
    }

    /**
     * Deinitializes the logger by closing the log file writer.
     */
    public static void deinit() {
        AgentLogger.debug("Deinitializing the AgentLogger");
        close();
    }

    /**
     * Dumps the stack trace of an exception to the log file.
     *
     * @param e The exception to dump.
     */
    public static void dumpException(Exception e) {
        StackTraceElement[] stackTraceElements = e.getStackTrace();
        StringBuilder stackTrace = new StringBuilder();
        for(StackTraceElement stackTraceElement: stackTraceElements) {
            stackTrace.append(stackTraceElement).append("\n");
        }
        draw(stackTrace.toString());
    }
}
