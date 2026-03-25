package com.asm.mja;

import com.asm.mja.config.Config;
import com.asm.mja.config.ConfigParser;
import com.asm.mja.metrics.MetricsHttpServer;
import com.asm.mja.monitor.*;
import com.asm.mja.rule.Rule;
import com.asm.mja.rule.RuleParser;
import com.asm.mja.rule.ReplacementSourceType;
import com.asm.mja.logging.TraceFileLogger;
import com.asm.mja.transformer.GlobalTransformer;
import com.asm.mja.utils.ByteCodeUtils;
import com.asm.mja.utils.ClassRuleUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.instrument.ClassDefinition;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

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

    private JVMCPUMonitor jvmCpuMonitor;

    private JVMGCMonitor jvmGcMonitor;

    private JVMThreadMonitor jvmThreadMonitor;

    private JVMClassLoaderMonitor jvmClassLoaderMonitor;

    private Config initialConfig;

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

    public void setCurrentRules(List<Rule> currentRules) {
        this.currentRules = currentRules;
    }

    public void setConfigRefreshInterval(Long configRefreshInterval) {this.configRefreshInterval = configRefreshInterval; }

    public void setInitialConfig(Config config) {this.initialConfig = config;}

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
                    startOrStopMonitors(config);
                    handleConfigurationChange(config);
                    lastModified = currentLastModified;
                } catch (IOException e) {
                    logger.error("Configuration file parsing failed, please verify if it is a valid YAML file after your changes", e);
                } catch (Throwable t) {
                    logger.error("Unexpected error while processing config reload; agent will keep running with previous state. Reason: " + t.getMessage());
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
        boolean instrumentationActive = config.isInstrumentationActive();

        if (!instrumentationActive) {
            disableInstrumentationIfActive(config);
            return;
        }

        if (transformer == null) {
            logger.warn("Instrumentation was not initialized at startup, so it cannot be enabled via config reload.");
            initialConfig = config;
            return;
        }

        if (!isBackupDirAvailable()) {
            logger.warn("No backup available, won't proceed with reverting instrumentations");
            initialConfig = config;
            return;
        }

        List<Rule> rules = currentRules == null ? Collections.<Rule>emptyList() : new ArrayList<>(currentRules);
        resetTransformerState();
        revertInstrumentation(rules);

        transformer.resetConfig(config);
        List<String> rulesString = new ArrayList<>(config.getAgentRules());
        List<Rule> newRules;
        try {
            newRules = RuleParser.parseRules(rulesString);
        } catch (Throwable t) {
            logger.error("Failed to parse new rules during config reload; keeping previous instrumentation. Reason: " + t.getMessage());
            return;
        }
        addNewInstrumentation(newRules);
        currentRules = newRules;
        initialConfig = config;
    }

    private boolean isBackupDirAvailable() {
        String backupDir = logger.getTraceDir() + File.separator + "backup";
        return new File(backupDir).exists();
    }

    private void resetTransformerState() {
        if (transformer == null) {
            return;
        }
        transformer.resetClassesTransformed();
        transformer.resetRules();
    }

    private void addNewInstrumentation(List<Rule> newRules) {
        List<Rule> transformerRules = newRules.stream()
                .filter(rule -> !rule.isClassReplacementRule())
                .collect(Collectors.toList());
        List<Rule> replacementRules = newRules.stream()
                .filter(Rule::isClassReplacementRule)
                .collect(Collectors.toList());

        applyTransformerRules(transformerRules);
        applyClassReplacementRules(replacementRules);
    }

    private void applyTransformerRules(List<Rule> transformerRules) {
        if (transformer == null) {
            return;
        }
        transformer.setRules(transformerRules);
        Class<?>[] classesToInstrument = ClassRuleUtils.ruleClasses(instrumentation.getAllLoadedClasses(), transformerRules);
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

    private void applyClassReplacementRules(List<Rule> replacementRules) {
        if (replacementRules == null || replacementRules.isEmpty()) {
            return;
        }

        for (Rule replacementRule : replacementRules) {
            List<Class<?>> targetClasses = ClassRuleUtils.resolveRuleClasses(
                    instrumentation.getAllLoadedClasses(),
                    Collections.singletonList(replacementRule)
            );

            if (targetClasses.isEmpty()) {
                logger.warn("Class replacement skipped; no loaded class matched pattern " + replacementRule.getClassName());
                continue;
            }

            for (Class<?> targetClass : targetClasses) {
                applySingleClassReplacement(replacementRule, targetClass);
            }
        }
    }

    private void applySingleClassReplacement(Rule replacementRule, Class<?> targetClass) {
        String sourcePath = replacementRule.getReplacementSourcePath();
        try {
            logger.trace("Class replacement requested for " + targetClass.getName() + " using "
                    + replacementRule.getReplacementSourceType() + " source: " + sourcePath);

            backupOriginalClassBytecodeIfMissing(targetClass);
            byte[] replacementBytecode = readReplacementBytecode(replacementRule, targetClass.getName());
            instrumentation.redefineClasses(new ClassDefinition(targetClass, replacementBytecode));
            logger.trace("Class replacement succeeded for " + targetClass.getName());
        } catch (Exception e) {
            logger.error("Class replacement failed for " + targetClass.getName() + " from " + sourcePath + "; reason: " + e.getMessage(), e);
        } catch (Error error) {
            logger.error("Class replacement failed for " + targetClass.getName() + " from " + sourcePath + "; reason: " + error.getMessage());
        }
    }

    private void revertInstrumentation(List<Rule> currentRules) {
        if (currentRules != null) {
            Set<String> classSet = ClassRuleUtils.resolveRuleClasses(instrumentation.getAllLoadedClasses(), currentRules)
                    .stream()
                    .map(Class::getName)
                    .collect(Collectors.toSet());
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

    private void backupOriginalClassBytecodeIfMissing(Class<?> targetClass) throws IOException {
        Path backupClassPath = Paths.get(constructBackupClassPath(targetClass.getName()));
        Files.createDirectories(backupClassPath.getParent());
        if (Files.exists(backupClassPath)) {
            return;
        }
        byte[] originalByteCode = ByteCodeUtils.getClassBytecode(targetClass);
        Files.write(backupClassPath, originalByteCode);
        logger.trace("Backed up class " + targetClass.getName() + " to " + backupClassPath.toAbsolutePath());
    }

    private byte[] readReplacementBytecode(Rule replacementRule, String className) throws IOException {
        ReplacementSourceType sourceType = replacementRule.getReplacementSourceType();
        String sourcePath = replacementRule.getReplacementSourcePath();
        if (sourceType == ReplacementSourceType.FILE) {
            return Files.readAllBytes(Paths.get(sourcePath));
        }

        if (sourceType == ReplacementSourceType.JAR) {
            return readClassFromJar(sourcePath, className);
        }

        throw new IllegalArgumentException("Unsupported replacement source type: " + sourceType);
    }

    private byte[] readClassFromJar(String jarPath, String className) throws IOException {
        String classEntryPath = className.replace('.', '/') + ".class";
        try (JarFile jarFile = new JarFile(jarPath)) {
            JarEntry jarEntry = jarFile.getJarEntry(classEntryPath);
            if (jarEntry == null) {
                throw new IOException("Class " + className + " not found in jar " + jarPath);
            }
            try (InputStream inputStream = jarFile.getInputStream(jarEntry)) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    baos.write(buffer, 0, bytesRead);
                }
                return baos.toByteArray();
            }
        }
    }

    public void execute() {
        logger.trace("Starting Monarch Instrumentation Manager");
        startOrStopMonitors(initialConfig);
        thread = new Thread(this, "monarch-inst-manager");
        thread.setDaemon(true);
        thread.start();
    }

    public void applyReplacementRulesOnStartup() {
        if (currentRules == null || currentRules.isEmpty()) {
            return;
        }
        List<Rule> replacementRules = currentRules.stream()
                .filter(Rule::isClassReplacementRule)
                .collect(Collectors.toList());
        applyClassReplacementRules(replacementRules);
    }

    public void shutdown() {

        if (jvmMemoryMonitor != null) jvmMemoryMonitor.shutdown();
        if (jvmCpuMonitor != null) jvmCpuMonitor.shutdown();
        if (jvmGcMonitor != null) jvmGcMonitor.shutdown();
        if (jvmThreadMonitor != null) jvmThreadMonitor.shutdown();
        if (jvmClassLoaderMonitor != null) jvmClassLoaderMonitor.shutdown();

        if (thread != null) {
            thread.interrupt();
        }

        logger.close();
    }


    private void startOrStopMonitors(Config config) {
        if (config == null) return;

        if (!config.isObserverActive()) {
            MetricsHttpServer.getInstance().stop();
            shutdownMonitor(jvmMemoryMonitor);
            jvmMemoryMonitor = null;
            shutdownMonitor(jvmCpuMonitor);
            jvmCpuMonitor = null;
            shutdownMonitor(jvmGcMonitor);
            jvmGcMonitor = null;
            shutdownMonitor(jvmThreadMonitor);
            jvmThreadMonitor = null;
            shutdownMonitor(jvmClassLoaderMonitor);
            jvmClassLoaderMonitor = null;
            return;
        }

        if (config.isExposeMetrics()) {
            MetricsHttpServer.getInstance().start(config.getMetricsPort());
        } else {
            MetricsHttpServer.getInstance().stop();
        }

        if (config.isPrintJVMHeapUsage()) {
            if (jvmMemoryMonitor == null || jvmMemoryMonitor.isDown()) {
                jvmMemoryMonitor = JVMMemoryMonitor.getInstance();
                jvmMemoryMonitor.setLogger(logger);
                jvmMemoryMonitor.execute();
            }

            if(jvmMemoryMonitor != null)
                jvmMemoryMonitor.setExposeMetrics(config.isExposeMetrics());

        } else {
            if (jvmMemoryMonitor != null && !jvmMemoryMonitor.isDown()) {
                jvmMemoryMonitor.shutdown();
                jvmMemoryMonitor = null;
            }
        }

        if (config.isPrintJVMCpuUsage()) {
            if (jvmCpuMonitor == null || jvmCpuMonitor.isDown()) {
                jvmCpuMonitor = JVMCPUMonitor.getInstance();
                jvmCpuMonitor.setLogger(logger);
                jvmCpuMonitor.execute();
            }

            if(jvmCpuMonitor != null)
                jvmCpuMonitor.setExposeMetrics(config.isExposeMetrics());

        } else {
            if (jvmCpuMonitor != null && !jvmCpuMonitor.isDown()) {
                jvmCpuMonitor.shutdown();
                jvmCpuMonitor = null;
            }
        }

        if (config.isPrintJVMGCStats()) {
            if (jvmGcMonitor == null || jvmGcMonitor.isDown()) {
                jvmGcMonitor = JVMGCMonitor.getInstance();
                jvmGcMonitor.setLogger(logger);
                jvmGcMonitor.execute();
            }

            if(jvmGcMonitor != null)
                jvmGcMonitor.setExposeMetrics(config.isExposeMetrics());

        } else {
            if (jvmGcMonitor != null && !jvmGcMonitor.isDown()) {
                jvmGcMonitor.shutdown();
                jvmGcMonitor = null;
            }
        }

        if (config.isPrintJVMThreadUsage()) {
            if (jvmThreadMonitor == null || jvmThreadMonitor.isDown()) {
                jvmThreadMonitor = JVMThreadMonitor.getInstance();
                jvmThreadMonitor.setLogger(logger);
                jvmThreadMonitor.execute();
            }

            if(jvmThreadMonitor != null)
                jvmThreadMonitor.setExposeMetrics(config.isExposeMetrics());

        } else {
            if (jvmThreadMonitor != null && !jvmThreadMonitor.isDown()) {
                jvmThreadMonitor.shutdown();
                jvmThreadMonitor = null;
            }
        }

        if (config.isPrintJVMClassLoaderStats()) {
            if (jvmClassLoaderMonitor == null || jvmClassLoaderMonitor.isDown()) {
                jvmClassLoaderMonitor = JVMClassLoaderMonitor.getInstance();
                jvmClassLoaderMonitor.setLogger(logger);
                jvmClassLoaderMonitor.execute();
            }

            if (jvmClassLoaderMonitor != null)
                jvmClassLoaderMonitor.setExposeMetrics(config.isExposeMetrics());

        } else {
            if (jvmClassLoaderMonitor != null && !jvmClassLoaderMonitor.isDown()) {
                jvmClassLoaderMonitor.shutdown();
                jvmClassLoaderMonitor = null;
            }
        }
    }

    private void disableInstrumentationIfActive(Config config) {
        if (transformer == null || currentRules == null || currentRules.isEmpty()) {
            initialConfig = config;
            currentRules = Collections.emptyList();
            return;
        }

        if (!isBackupDirAvailable()) {
            logger.warn("No backup available, won't proceed with reverting instrumentations");
            initialConfig = config;
            currentRules = Collections.emptyList();
            return;
        }

        List<Rule> rules = new ArrayList<>(currentRules);
        resetTransformerState();
        revertInstrumentation(rules);
        currentRules = Collections.emptyList();
        initialConfig = config;
        logger.trace("Instrumentation disabled by config reload");
    }

    private void shutdownMonitor(AbstractMonitor monitor) {
        if (monitor != null && !monitor.isDown()) {
            monitor.shutdown();
        }
    }

}
