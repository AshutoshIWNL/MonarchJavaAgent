package com.asm.mja;

import com.asm.mja.config.Config;
import com.asm.mja.config.ConfigParser;
import com.asm.mja.rule.Rule;
import com.asm.mja.rule.RuleParser;
import com.asm.mja.logging.TraceFileLogger;
import com.asm.mja.monitor.JVMMemoryMonitor;
import com.asm.mja.transformer.GlobalTransformer;
import com.asm.mja.utils.ByteCodeUtils;
import com.asm.mja.utils.ClassRuleUtils;

import java.io.File;
import java.io.IOException;
import java.lang.instrument.ClassDefinition;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author ashut
 * @since 15-08-2024
 */

public class InstrumentationManager implements Runnable {
    private TraceFileLogger logger;
    private Thread thread = null;
    private String configFilePath;

    private long lastModified;

    private Instrumentation instrumentation;

    private List<Rule> currentRules;

    private JVMMemoryMonitor jvmMemoryMonitor;

    private static InstrumentationManager instance = null;
    private GlobalTransformer transformer;

    private long configRefreshInterval;

    // Cache for original bytecode to avoid redundant file I/O
    private final Map<String, byte[]> bytecodeCache = new HashMap<>();

    public static InstrumentationManager getInstance() {
        if(instance == null) {
            instance = new InstrumentationManager();
        }
        return instance;
    }
    public InstrumentationManager() {

    }

    public void setLogger(TraceFileLogger logger) {
        this.logger = logger;
    }

    public void setConfigFilePath(String configFilePath) {
        this.configFilePath = configFilePath;
    }

    public void setLastModified(long lastModified) {
        this.lastModified = lastModified;
    }

    public void setInstrumentation(Instrumentation instrumentation) {
        this.instrumentation = instrumentation;
    }

    public void setTransformer(GlobalTransformer transformer) {
        this.transformer = transformer;
    }

    public void setJvmMemoryMonitor(JVMMemoryMonitor jvmMemoryMonitor) {
        this.jvmMemoryMonitor = jvmMemoryMonitor;
    }

    public void setCurrentRules(List<Rule> currentRules) {
        this.currentRules = currentRules;
    }

    public void setConfigRefreshInterval(Long configRefreshInterval) {this.configRefreshInterval = configRefreshInterval; }

    @Override
    public void run() {
        while (true) {
            File file = new File(configFilePath);
            long currentLastModified = file.lastModified();

            if (currentLastModified != lastModified) {
                Config config = null;
                try {
                    config = ConfigParser.parse(configFilePath, logger);
                    configRefreshInterval = config.getConfigRefreshInterval();
                    logger.trace("Configuration file has been modified, re-parsing it");
                    handleConfigurationChange(config);
                    lastModified = currentLastModified;
                } catch (IOException e) {
                    logger.error("Configuration file parsing failed, please verify if it is a valid YAML file after your changes", e);
                }
            }

            try {
                Thread.sleep(configRefreshInterval);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.trace("Instrumentation Manager thread interrupted");
                break;
            }
        }
    }

    private void handleConfigurationChange(Config config) {
        if (!isBackupDirAvailable()) {
            logger.warn("No backup available, won't proceed with reverting instrumentations");
            return;
        }

        List<Rule> rules = new ArrayList<>(currentRules);
        resetTransformerState();
        revertInstrumentation(rules);

        if (!config.shouldInstrument()) {
            logger.trace("Going to shutdown instrumentation");
            shutdown();
            return;
        }
        transformer.resetConfig(config);
        List<String> rulesString = new ArrayList<>(config.getAgentRules());
        List<Rule> newRules = RuleParser.parseRules(rulesString);
        addNewInstrumentation(newRules);
        currentRules = newRules;
    }

    private boolean isBackupDirAvailable() {
        String backupDir = logger.getTraceDir() + File.separator + "backup";
        return new File(backupDir).exists();
    }

    private void resetTransformerState() {
        transformer.resetClassesTransformed();
        transformer.resetRules();
    }

    private void addNewInstrumentation(List<Rule> newRules) {
        transformer.setRules(newRules);
        Class<?>[] classesToInstrument = ClassRuleUtils.ruleClasses(instrumentation.getAllLoadedClasses(), newRules);
        for (Class<?> classz : classesToInstrument) {
            String className = classz.getName();
            try {
                /*
                 Using redefine here because re-transform will take the modified byte code as its source and would then result in changes which aren't intended
                 whereas I can pass the source for redefine myself
                 */
                if(bytecodeCache.containsKey(className))
                    instrumentation.redefineClasses(new ClassDefinition(classz, bytecodeCache.get(className)));
                else {
                    //First time for this class: Get the original byte code. Going forward, it will be put in bytecode-cache, so, it will be used from there
                    instrumentation.redefineClasses(new ClassDefinition(classz, ByteCodeUtils.getClassBytecode(classz)));
                }
            } catch (UnmodifiableClassException | ClassNotFoundException | IOException e) {
                logger.error("Failed to re-transform classes;" + "Exception: " + e.getMessage(), e);
            }
        }
    }

    private void revertInstrumentation(List<Rule> currentRules) {
        if (currentRules != null) {
            Set<String> classSet = currentRules.stream().map(Rule::getClassName).collect(Collectors.toSet());
            for (String cName : classSet) {
                loadOriginalByteCode(cName);
            }
        }
        logger.trace("Reverted previous instrumentation");
    }

    private Class<?> findLoadedClass(Instrumentation instrumentation, String className) throws ClassNotFoundException {
        for (Class<?> loadedClass : instrumentation.getAllLoadedClasses()) {
            if (loadedClass.getName().equals(className)) {
                return loadedClass;
            }
        }
        throw new ClassNotFoundException("Class not found in loaded classes: " + className);
    }

    private void loadOriginalByteCode(String className) {
        String backupClassPath = constructBackupClassPath(className);
        try {
            byte[] originalBytecode = Files.readAllBytes(Paths.get(backupClassPath));
            bytecodeCache.put(className, originalBytecode);
            Class<?> targetClass = findLoadedClass(instrumentation, className);
            instrumentation.redefineClasses(new ClassDefinition(targetClass, originalBytecode));
        } catch (IOException | UnmodifiableClassException | ClassNotFoundException e) {
            logger.error("Failed to read bytecode for class " + className + "; Exception: " + e.getMessage(), e);
        }
    }

    private String constructBackupClassPath(String className) {
        String safeFileName = className.replace('.', '_').replace('$', '_') + ".class";
        return logger.getTraceDir() + File.separator + "backup" + File.separator + safeFileName;
    }

    public void execute() {
        logger.trace("Starting Monarch Instrumentation Manager");
        thread = new Thread(this, "monarch-inst-manager");
        thread.setDaemon(true);
        thread.start();
    }

    public void shutdown() {
        if(jvmMemoryMonitor != null) {
            jvmMemoryMonitor.shutdown();
        }
        if(thread != null) {
            logger.trace("Shutting down Monarch Instrumentation Manager");
            thread.interrupt();
        }
        logger.close();
    }
}
