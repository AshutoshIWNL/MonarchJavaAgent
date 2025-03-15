package com.asm.mja.logging;

import com.asm.mja.utils.DateUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author ashut
 * @since 11-04-2024
 */

public class TraceFileLogger {

    private static TraceFileLogger instance;
    private static final String LOG_FILE_NAME = "agent.trace";
    private String fileName;
    private PrintWriter writer;
    private final Lock lock = new ReentrantLock();

    public String traceDir;

    public void init(String location) {
        fileName = location + File.separator + LOG_FILE_NAME;
        traceDir = location;
        try {
            writer = new PrintWriter(new FileWriter(fileName, true));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getTraceDir() {
        return traceDir;
    }

    private TraceFileLogger() {

    }

    public static TraceFileLogger getInstance() {
        if(instance == null) {
            synchronized (TraceFileLogger.class) {
                if (instance == null) {
                    instance = new TraceFileLogger();
                }
            }
        }
        return instance;
    }

    public void trace(String message) {
        if (writer == null) {
            throw new IllegalStateException("TraceFileLogger has not been initialized. Call init() first.");
        }
        StringBuilder logMessage = new StringBuilder();
        logMessage.append(DateUtils.getFormattedTimestamp()).append(' ');
        logMessage.append("[TRACE] ");
        logMessage.append('[').append(Thread.currentThread().getName()).append("] ");
        logMessage.append(message);
        writeLog(logMessage.toString());
    }

    public void error(String message) {
        if (writer == null) {
            throw new IllegalStateException("TraceFileLogger has not been initialized. Call init() first.");
        }
        StringBuilder logMessage = new StringBuilder();
        logMessage.append(DateUtils.getFormattedTimestamp()).append(' ');
        logMessage.append("[ERROR] ");
        logMessage.append('[').append(Thread.currentThread().getName()).append("] ");
        logMessage.append(message);
        writeLog(logMessage.toString());
    }

    public void error(String message, Exception e) {
        if (writer == null) {
            throw new IllegalStateException("TraceFileLogger has not been initialized. Call init() first.");
        }
        StringBuilder logMessage = new StringBuilder();
        logMessage.append(DateUtils.getFormattedTimestamp()).append(' ');
        logMessage.append("[ERROR] ");
        logMessage.append('[').append(Thread.currentThread().getName()).append("] ");
        logMessage.append(message);
        writeLog(logMessage.toString());
        exception(e);
    }

    public void warn(String message) {
        if (writer == null) {
            throw new IllegalStateException("TraceFileLogger has not been initialized. Call init() first.");
        }
        StringBuilder logMessage = new StringBuilder();
        logMessage.append(DateUtils.getFormattedTimestamp()).append(' ');
        logMessage.append("[WARN] ");
        logMessage.append('[').append(Thread.currentThread().getName()).append("] ");
        logMessage.append(message);
        writeLog(logMessage.toString());
    }

    public void stack(String message, StackTraceElement[] stackTraceElements) {
        if (writer == null) {
            throw new IllegalStateException("TraceFileLogger has not been initialized. Call init() first.");
        }
        StringBuilder logMessage = new StringBuilder();
        logMessage.append(DateUtils.getFormattedTimestamp()).append(' ');
        logMessage.append("[TRACE] ");
        logMessage.append('[').append(Thread.currentThread().getName()).append("] ");
        logMessage.append(message).append("\n");
        for (StackTraceElement element : stackTraceElements) {
            if(element.toString().startsWith("java.lang.Thread.getStackTrace"))
                continue;
            logMessage.append("\tat ").append(element.toString()).append('\n');
        }
        writeLog(logMessage.toString());
    }

    public void exception(Exception e) {
        if (writer == null) {
            throw new IllegalStateException("TraceFileLogger has not been initialized. Call init() first.");
        }
        StringBuilder logMessage = new StringBuilder();
        logMessage.append(DateUtils.getFormattedTimestamp()).append(' ');
        logMessage.append("[EXCEPTION] ");
        logMessage.append('[').append(Thread.currentThread().getName()).append("] ");
        logMessage.append(e.getMessage());
        for (StackTraceElement element : e.getStackTrace()) {
            logMessage.append("\tat ").append(element.toString()).append("\n");
        }
        writeLog(logMessage.toString());
    }

    private void writeLog(String message) {
        lock.lock();
        try {
            writer.println(message);
            writer.flush();
        } finally {
            lock.unlock();
        }
    }

    public void close() {
        if (writer != null) {
            trace("Shutting down TraceFileLogger");
            writer.close();
        }
    }

}
