package com.asm.mja.transformer;

import com.asm.mja.config.Config;
import com.asm.mja.exception.BackupCreationException;
import com.asm.mja.exception.TransformException;
import com.asm.mja.exception.UnsupportedActionException;
import com.asm.mja.logging.TraceFileLogger;
import com.asm.mja.rule.Rule;
import com.asm.mja.transformer.handlers.*;
import com.asm.mja.utils.ClassLoaderTracer;
import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.LoaderClassPath;
import javassist.NotFoundException;

import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.ProtectionDomain;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The GlobalTransformer class implements the ClassFileTransformer interface
 * to perform bytecode transformation on loaded classes.
 * It is responsible for applying transformations based on the provided configuration.
 *
 * @author ashut
 * @since 11-04-2024
 */
public class GlobalTransformer implements ClassFileTransformer {

    private Config config;
    private List<Rule> rules;
    private Map<String, List<Rule>> rulesByClassName = new ConcurrentHashMap<>();
    private final TraceFileLogger logger;
    private final Set<String> classesTransformed = ConcurrentHashMap.newKeySet();
    private final Set<String> backupSet = ConcurrentHashMap.newKeySet();
    private static final String MJA_PACKAGE = "com/asm/mja";
    private final String mode;
    private final String agentAbsolutePath;
    private static final boolean isJdk9OrLater = Integer.parseInt(System.getProperty("java.version").split("\\.")[0]) >= 9;

    private final Map<Action, ActionHandler> actionHandlers = new EnumMap<>(Action.class);
    private final ActionHandler profileActionHandler;

    public void resetConfig(Config config) {
        this.config = config;
    }

    /**
     * Constructs a GlobalTransformer with the specified configuration.
     *
     * @param config The configuration object.
     */
    public GlobalTransformer(Config config, TraceFileLogger logger, List<Rule> rules, String mode, String agentAbsolutePath) {
        this.config = config;
        this.logger = logger;
        setRules(rules);
        this.mode = mode;
        this.agentAbsolutePath = agentAbsolutePath;

        ClassPoolProvider classPoolProvider = this::getClassPool;
        actionHandlers.put(Action.ARGS, new ArgsActionHandler(classPoolProvider));
        actionHandlers.put(Action.STACK, new StackActionHandler(classPoolProvider));
        actionHandlers.put(Action.HEAP, new HeapActionHandler(classPoolProvider));
        actionHandlers.put(Action.RET, new ReturnActionHandler(classPoolProvider, logger));
        actionHandlers.put(Action.ADD, new CustomCodeActionHandler(classPoolProvider));
        profileActionHandler = new ProfileActionHandler(classPoolProvider);
    }

    /**
     * Transforms the bytecode of a loaded class.
     *
     * @param loader              The classloader loading the class.
     * @param className           The name of the class being transformed.
     * @param classBeingRedefined The class being redefined, if applicable.
     * @param protectionDomain    The protection domain of the class.
     * @param classfileBuffer     The bytecode of the class.
     * @return The transformed bytecode.
     * @throws IllegalClassFormatException If the class file format is illegal or unsupported.
     */
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
                            ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
        if (className == null) {
            logger.warn("Received null className during transformation. Loader: " + loader);
            return classfileBuffer;
        }
        if (config.isPrintClassLoaderTrace() &&
                !className.startsWith("java/") && !className.startsWith("jdk/") &&
                !className.startsWith("sun/") && !className.startsWith("javax/") && !className.startsWith(MJA_PACKAGE)) {
            logger.trace(ClassLoaderTracer.printClassInfo(className, loader, protectionDomain));
        }
        if (rules.isEmpty()) {
            return classfileBuffer;
        }

        String formattedClassName = className.replace("/", ".");
        List<Rule> appropriateRules = getAppropriateRules(formattedClassName);
        boolean needsInstrumentation = !appropriateRules.isEmpty();
        try {
            if (needsInstrumentation) {
                if (!backupSet.contains(formattedClassName)) {
                    backupByteCode(formattedClassName, classfileBuffer, logger.getTraceDir());
                }
                return transformClass(formattedClassName, classfileBuffer, appropriateRules);
            }
        } catch (TransformException e) {
            logger.error("Failed to transform class " + formattedClassName, e);
        } catch (BackupCreationException e) {
            logger.error("Failed to back up bytecode for class " + formattedClassName + ", won't go ahead with the transformation", e);
        }
        return classfileBuffer;
    }

    private List<Rule> getAppropriateRules(String formattedClassName) {
        List<Rule> result = rulesByClassName.get(formattedClassName.toLowerCase());
        return result != null ? result : Collections.emptyList();
    }

    public void resetClassesTransformed() {
        this.classesTransformed.clear();
    }

    public void resetRules() {
        this.rules.clear();
        this.rulesByClassName.clear();
    }

    public void setRules(List<Rule> rules) {
        this.rules = rules;
        Map<String, List<Rule>> newMap = new ConcurrentHashMap<>();
        for (Rule rule : rules) {
            String className = rule.getClassName().toLowerCase();
            newMap.computeIfAbsent(className, k -> new ArrayList<>()).add(rule);
        }
        this.rulesByClassName = newMap;
    }

    private void backupByteCode(String formattedClassName, byte[] classFileBuffer, String traceDir) throws BackupCreationException {
        try {
            Objects.requireNonNull(formattedClassName, "Class name cannot be null");
            Objects.requireNonNull(traceDir, "Trace directory cannot be null");

            Path backupDirPath = Paths.get(traceDir, "backup");
            Files.createDirectories(backupDirPath);

            String safeFileName = formattedClassName.replace('.', '_').replace('$', '_') + ".class";
            Path classFilePath = backupDirPath.resolve(safeFileName);

            Files.write(classFilePath, classFileBuffer);
            backupSet.add(formattedClassName);

            logger.trace("Backed up class " + formattedClassName + " to " + classFilePath.toAbsolutePath());
        } catch (IOException e) {
            throw new BackupCreationException("Failed to back up class " + formattedClassName, e);
        }
    }

    private byte[] transformClass(String formattedClassName, byte[] modifiedBytes, List<Rule> rules) throws TransformException {
        if (classesTransformed.contains(formattedClassName)) {
            logger.trace("Re-transforming class " + formattedClassName);
        } else {
            logger.trace("Going to transform class " + formattedClassName);
            classesTransformed.add(formattedClassName);
        }

        for (Rule rule : rules) {
            try {
                modifiedBytes = applyRule(rule, formattedClassName, modifiedBytes);
            } catch (IOException | CannotCompileException | UnsupportedActionException | NotFoundException e) {
                logger.error(e.getMessage(), e);
                throw new TransformException(e);
            }
        }
        logger.trace("Transformed class " + formattedClassName);
        return modifiedBytes;
    }

    private byte[] applyRule(Rule rule, String formattedClassName, byte[] modifiedBytes) throws IOException, CannotCompileException, UnsupportedActionException, NotFoundException {
        ActionExecution execution = new ActionExecution(
                rule.getMethodName(),
                rule.getEvent(),
                rule.getAction(),
                rule.getCustomCode(),
                rule.getFilterName(),
                formattedClassName,
                modifiedBytes,
                rule.getLineNumber()
        );

        if (rule.getEvent().equals(Event.PROFILE)) {
            return profileActionHandler.apply(execution);
        }

        ActionHandler handler = actionHandlers.get(rule.getAction());
        if (handler == null) {
            return modifiedBytes;
        }
        return handler.apply(execution);
    }

    private ClassPool getClassPool() throws NotFoundException {
        ClassPool pool = ClassPool.getDefault();
        if (isJdk9OrLater && mode.equals("attachVM")) {
            pool.appendClassPath(agentAbsolutePath);
            pool.appendClassPath(new LoaderClassPath(ClassLoader.getSystemClassLoader()));
        }
        return pool;
    }
}
