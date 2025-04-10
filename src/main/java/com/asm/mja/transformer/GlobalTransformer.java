package com.asm.mja.transformer;

import com.asm.mja.config.Config;
import com.asm.mja.exception.BackupCreationException;
import com.asm.mja.exception.TransformException;
import com.asm.mja.exception.UnsupportedActionException;
import com.asm.mja.rule.Rule;
import com.asm.mja.logging.TraceFileLogger;
import com.asm.mja.utils.ClassLoaderTracer;
import javassist.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
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
    private final TraceFileLogger logger;
    private Set<String> classesTransformed = ConcurrentHashMap.newKeySet();
    private Set<String> backupSet = ConcurrentHashMap.newKeySet();
    private static final String MJA_PACKAGE = "com/asm/mja";
    private final String mode;
    private final String agentAbsolutePath;
    private static final boolean isJdk9OrLater = Integer.parseInt(System.getProperty("java.version").split("\\.")[0]) >= 9;

    public void resetConfig(Config config) {
        this.config = config;
    }

    /**
     * Constructs a GlobalTransformer with the specified configuration.
     *
     * @param config     The configuration object.
     */
    public GlobalTransformer(Config config, TraceFileLogger logger, List<Rule> rules, String mode, String agentAbsolutePath) {
        this.config = config;
        this.logger = logger;
        this.rules = rules;
        this.mode = mode;
        this.agentAbsolutePath = agentAbsolutePath;
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
            return classfileBuffer; // Avoid processing null class names
        }
        //Skipping JDK classes because logging classloading events for JDK classes can cause circular dependencies (ClassCircularityError)
        if(config.isPrintClassLoaderTrace() &&
                !className.startsWith("java/") && !className.startsWith("jdk/") &&
                !className.startsWith("sun/") && !className.startsWith("javax/") && !className.startsWith(MJA_PACKAGE)) {
            logger.trace(ClassLoaderTracer.printClassInfo(className, loader, protectionDomain));
        }
        if(rules.isEmpty())
            return classfileBuffer;
        String formattedClassName = className.replace("/", ".");
        List<Rule> appropriateRules = getAppropriateRules(formattedClassName);
        boolean needsInstrumentation =  !appropriateRules.isEmpty();
        try {
            if(needsInstrumentation) {
                if(!backupSet.contains(formattedClassName))
                    backupByteCode(formattedClassName, classfileBuffer, logger.getTraceDir());
                return transformClass(loader, formattedClassName, classBeingRedefined, classfileBuffer, appropriateRules);
            }
        } catch (TransformException e) {
            logger.error("Failed to transform class " + formattedClassName, e);
        } catch (BackupCreationException e) {
            logger.error("Failed to back up bytecode for class " + formattedClassName + ", won't go ahead with the transformation", e);
        }
        return classfileBuffer;
    }

    private List<Rule> getAppropriateRules(String formattedClassName) {
        List<Rule> ruleList = new ArrayList<>();
        for(Rule rule: rules) {
            if(rule.getClassName().equalsIgnoreCase(formattedClassName)) {
                ruleList.add(rule);
            }
        }
        return ruleList;
    }

    public void resetClassesTransformed() {
        this.classesTransformed.clear();
    }

    public void resetRules() {
        this.rules.clear();
    }

    public void setRules(List<Rule> rules) {
        this.rules = rules;
    }

    private void backupByteCode(String formattedClassName, byte[] classFileBuffer, String traceDir) throws BackupCreationException {
        File backUpDir = new File(traceDir + File.separator + "backup");
        backUpDir.mkdirs();

        File classFile = new File(backUpDir, formattedClassName.substring(formattedClassName.lastIndexOf('.') + 1) + ".class");

        try (FileOutputStream fos = new FileOutputStream(classFile)) {
            fos.write(classFileBuffer);
            backupSet.add(formattedClassName);
        } catch (IOException e) {
            throw new BackupCreationException(e.getMessage(), e);
        }
    }

    private byte[] transformClass(ClassLoader loader, String formattedClassName,
                                  Class<?> classBeingRedefined, byte[] classfileBuffer, List<Rule> rules) throws TransformException {
        byte[] modifiedBytes = classfileBuffer;
        if(classesTransformed.contains(formattedClassName)) {
            logger.trace("Re-transforming class " + formattedClassName);
        } else {
            logger.trace("Going to transform class " + formattedClassName);
            classesTransformed.add(formattedClassName);
        }
        for(Rule rule: rules) {
            try {
                switch (rule.getEvent()) {
                    case INGRESS:
                        modifiedBytes = performIngressAction(rule.getMethodName(), rule.getAction(), rule.getCustomCode(), loader, formattedClassName, classBeingRedefined, modifiedBytes);
                        break;
                    case EGRESS:
                        modifiedBytes = performEgressAction(rule.getMethodName(), rule.getAction(), rule.getCustomCode(), loader, formattedClassName, classBeingRedefined, modifiedBytes);
                        break;
                    case CODEPOINT:
                        modifiedBytes = performCodePointAction(rule.getMethodName(), rule.getAction(), rule.getCustomCode(), loader, formattedClassName, classBeingRedefined, modifiedBytes, rule.getLineNumber());
                        break;
                    case PROFILE:
                        modifiedBytes = performProfiling(rule.getMethodName(), rule.getAction(), loader, formattedClassName, classBeingRedefined, modifiedBytes, rule.getLineNumber());
                }
            } catch (IOException | CannotCompileException | UnsupportedActionException | NotFoundException e) {
                logger.error(e.getMessage(), e);
                throw new TransformException(e);
            }
        }
        logger.trace("Transformed class " + formattedClassName);
        return modifiedBytes;
    }

    private ClassPool getClassPool() throws NotFoundException {
        ClassPool pool = ClassPool.getDefault();
        if (isJdk9OrLater && mode.equals("attachVM")) {
            // Fix for JDK 9+ visibility issue in agentmain mode:
            // Ensures java.lang classes are accessible by appending the system class loader to ClassPool.
            pool.appendClassPath(agentAbsolutePath);
            pool.appendClassPath(new LoaderClassPath(ClassLoader.getSystemClassLoader()));
        }
        return pool;
    }

    //Todo: Use ASM to do this as with JavaAssist, it sometimes throws VerifyError
    private byte[] performProfiling(String methodName, Action action, ClassLoader loader,
                                    String formattedClassName, Class<?> classBeingRedefined, byte[] modifiedBytes, int lineNumber) throws IOException, CannotCompileException, NotFoundException {
        ClassPool pool = getClassPool();
        CtClass ctClass = pool.makeClass(new java.io.ByteArrayInputStream(modifiedBytes));
        //addLoggerField(ctClass);
        for(CtMethod method : ctClass.getDeclaredMethods()) {
            if(method.getName().equals(methodName)) {
                // Declaring startTime as local variable to pass it to insertAfter (it won't work without this)
                method.addLocalVariable("startTime", CtClass.longType);
                method.insertBefore("try { startTime = System.nanoTime(); } catch(Exception e){}");

                method.insertAfter("com.asm.mja.logging.TraceFileLogger logger = com.asm.mja.logging.TraceFileLogger.getInstance(); try {" +
                        "    long endTime = System.nanoTime();" +
                        "    final long executionTime = (endTime - startTime) / 1000000;" +
                        "    logger.trace(\"{" + formattedClassName + '.' + methodName + "} | PROFILE | Execution time: \" + executionTime + \"ms\");" +
                        "} catch (Exception e) { }");

            }
        }
        // CtClass frozen - due to  writeFile()/toClass()/toBytecode()
        modifiedBytes = ctClass.toBytecode();

        // To remove from ClassPool
        ctClass.detach();
        return modifiedBytes;
    }

    private byte[] performCodePointAction(String methodName, Action action, String customCode, ClassLoader loader,
                                   String formattedClassName, Class<?> classBeingRedefined, byte[] modifiedBytes, int lineNumber) throws IOException, CannotCompileException, NotFoundException {
        switch (action) {
            case STACK:
                return getStack(methodName, Event.CODEPOINT, loader, formattedClassName, classBeingRedefined, modifiedBytes, lineNumber);
            case HEAP:
                return getHeap(methodName, Event.CODEPOINT, loader, formattedClassName, classBeingRedefined, modifiedBytes, lineNumber);
            case ADD:
                return addCustomCode(customCode, methodName, Event.CODEPOINT, loader, formattedClassName, classBeingRedefined, modifiedBytes, lineNumber);
        }
        return modifiedBytes;
    }

    private byte[] performEgressAction(String methodName, Action action, String customCode, ClassLoader loader,
                                     String formattedClassName, Class<?> classBeingRedefined, byte[] modifiedBytes) throws IOException, CannotCompileException, UnsupportedActionException, NotFoundException {
        switch (action) {
            case STACK:
                return getStack(methodName, Event.EGRESS, loader, formattedClassName, classBeingRedefined, modifiedBytes, 0);
            case HEAP:
                return getHeap(methodName, Event.EGRESS, loader, formattedClassName, classBeingRedefined, modifiedBytes, 0);
            case RET:
                return getReturnValue(methodName, Event.EGRESS, loader, formattedClassName, classBeingRedefined, modifiedBytes);
            case ADD:
                return addCustomCode(customCode, methodName, Event.EGRESS, loader, formattedClassName, classBeingRedefined, modifiedBytes, 0);
        }
        return modifiedBytes;
    }

    private byte[] performIngressAction(String methodName, Action action, String customCode, ClassLoader loader,
                                      String formattedClassName, Class<?> classBeingRedefined, byte[] modifiedBytes) throws IOException, CannotCompileException, UnsupportedActionException, NotFoundException {
        switch (action) {
            case STACK:
                return getStack(methodName, Event.INGRESS, loader, formattedClassName, classBeingRedefined, modifiedBytes, 0);
            case HEAP:
                return getHeap(methodName, Event.INGRESS, loader, formattedClassName, classBeingRedefined, modifiedBytes, 0);
            case ARGS:
                return getArgs(methodName, Event.INGRESS, loader, formattedClassName, classBeingRedefined, modifiedBytes);
            case ADD:
                return addCustomCode(customCode, methodName, Event.INGRESS, loader, formattedClassName, classBeingRedefined, modifiedBytes, 0);
        }
        return modifiedBytes;
    }

    private byte[] getArgs(String methodName, Event event, ClassLoader loader, String formattedClassName,
                           Class<?> classBeingRedefined, byte[] modifiedBytes) throws IOException, CannotCompileException, UnsupportedActionException, NotFoundException {
        ClassPool pool = getClassPool();
        CtClass ctClass = pool.makeClass(new java.io.ByteArrayInputStream(modifiedBytes));
        //addLoggerField(ctClass);
        if(formattedClassName.endsWith(methodName)) {
            for (CtConstructor constructor : ctClass.getConstructors()) {
                    StringBuilder code = new StringBuilder();
                    CtClass[] parameterTypes = new CtClass[0];
                    try {
                        parameterTypes = constructor.getParameterTypes();
                    } catch (NotFoundException ignored) {
                        //ignoring this
                    }

                    if (parameterTypes.length == 0) {
                        code.append("com.asm.mja.logging.TraceFileLogger logger = com.asm.mja.logging.TraceFileLogger.getInstance();");
                        code.append("try {");
                        code.append("    logger.trace(\"{").append(formattedClassName).append('.').append(methodName).append("} | ").append(event).append(" | ").append("ARGS | NULL\");");
                        code.append("} catch (Exception e) {}");
                    } else {
                        code.append("com.asm.mja.logging.TraceFileLogger logger = com.asm.mja.logging.TraceFileLogger.getInstance();");
                        code.append("try {");
                        code.append("    StringBuilder args = new StringBuilder(\"\");");
                        for (int i = 0; i < parameterTypes.length; i++) {
                            code.append("    args.append(\" ").append(i).append("=\").append(");
                            if (parameterTypes[i].isPrimitive()) {
                                code.append('$').append(i + 1);
                            } else {
                                code.append('$').append(i + 1).append(".toString()");
                            }
                            code.append(");");
                        }
                        code.append("    logger.trace(\"{").append(formattedClassName).append('.').append(methodName).append("} | ").append(event).append(" | ARGS | \" + args.toString());");
                        code.append("} catch (Exception e) {}");
                    }

                    if (event.equals(Event.INGRESS)) {
                        constructor.insertBefore(code.toString());
                    } else if (event.equals(Event.EGRESS)) {
                        throw new UnsupportedActionException("Getting arguments for EGRESS is not supported");
                    } else {
                        throw new UnsupportedActionException("Getting arguments for CODEPOINT is not supported");
                    }
                }
            }
        else {
            for (CtMethod method : ctClass.getDeclaredMethods()) {
                if (method.getName().equals(methodName)) {
                    StringBuilder code = new StringBuilder();
                    CtClass[] parameterTypes = new CtClass[0];
                    try {
                        parameterTypes = method.getParameterTypes();
                    } catch (NotFoundException ignored) {
                        //ignoring this
                    }

                    if (parameterTypes.length == 0) {
                        code.append("com.asm.mja.logging.TraceFileLogger logger = com.asm.mja.logging.TraceFileLogger.getInstance();");
                        code.append("try {");
                        code.append("    logger.trace(\"{").append(formattedClassName).append('.').append(methodName).append("} | ").append(event).append(" | ").append("ARGS | NULL\");");
                        code.append("} catch (Exception e) {}");
                    } else {
                        code.append("com.asm.mja.logging.TraceFileLogger logger = com.asm.mja.logging.TraceFileLogger.getInstance();");
                        code.append("try {");
                        code.append("    StringBuilder args = new StringBuilder(\"\");");
                        for (int i = 0; i < parameterTypes.length; i++) {
                            code.append("    args.append(\" ").append(i).append("=\").append(");
                            if (parameterTypes[i].isPrimitive()) {
                                code.append('$').append(i + 1);
                            } else {
                                code.append('$').append(i + 1).append(".toString()");
                            }
                            code.append(");");
                        }
                        code.append("    logger.trace(\"{").append(formattedClassName).append('.').append(methodName).append("} | ").append(event).append(" | ARGS | \" + args.toString());");
                        code.append("} catch (Exception e) {}");
                    }

                    if (event.equals(Event.INGRESS)) {
                        method.insertBefore(code.toString());
                    } else if (event.equals(Event.EGRESS)) {
                        throw new UnsupportedActionException("Getting arguments for EGRESS is not supported");
                    } else {
                        throw new UnsupportedActionException("Getting arguments for CODEPOINT is not supported");
                    }
                }
            }
        }

        // CtClass frozen - due to  writeFile()/toClass()/toBytecode()
        modifiedBytes = ctClass.toBytecode();

        // To remove from ClassPool
        ctClass.detach();
        return modifiedBytes;
    }


    private byte[] getStack(String methodName, Event event, ClassLoader loader,
                            String formattedClassName, Class<?> classBeingRedefined, byte[] modifiedBytes, int lineNumber) throws IOException, CannotCompileException, NotFoundException {
        ClassPool pool = getClassPool();
        CtClass ctClass = pool.makeClass(new java.io.ByteArrayInputStream(modifiedBytes));
        //addLoggerField(ctClass);
        if(formattedClassName.endsWith(methodName)) {
            for(CtConstructor constructor: ctClass.getConstructors()) {
                String insertString = "com.asm.mja.logging.TraceFileLogger logger = com.asm.mja.logging.TraceFileLogger.getInstance(); try { " +
                            "logger.stack(\"{" + formattedClassName + '.' + methodName + "} | " + event + " | " + "STACK\"" + ", new Throwable().getStackTrace()); " +
                            "} catch (Exception e) {}";
                if(event.equals(Event.INGRESS))
                    constructor.insertBefore(insertString);
                else if(event.equals(Event.EGRESS))
                    constructor.insertAfter(insertString);
                else
                    constructor.insertAt(lineNumber, insertString);
            }
        } else {
            for(CtMethod method : ctClass.getDeclaredMethods()) {
                if(method.getName().equals(methodName)) {
                    String insertString = "com.asm.mja.logging.TraceFileLogger logger = com.asm.mja.logging.TraceFileLogger.getInstance(); try { " +
                            "logger.stack(\"{" + formattedClassName + '.' + methodName + "} | " + event + " | " + "STACK\"" + ", new Throwable().getStackTrace()); " +
                            "} catch (Exception e) {}";
                    if(event.equals(Event.INGRESS))
                        method.insertBefore(insertString);
                    else if(event.equals(Event.EGRESS))
                        method.insertAfter(insertString);
                    else
                        method.insertAt(lineNumber, insertString);
                }
            }
        }
        // CtClass frozen - due to  writeFile()/toClass()/toBytecode()
        modifiedBytes = ctClass.toBytecode();

        // To remove from ClassPool
        ctClass.detach();
        return modifiedBytes;
    }

    private byte[] getHeap(String methodName, Event event, ClassLoader loader,
                           String formattedClassName, Class<?> classBeingRedefined, byte[] modifiedBytes, int lineNumber) throws IOException, CannotCompileException, NotFoundException {
        ClassPool pool = getClassPool();
        CtClass ctClass = pool.makeClass(new java.io.ByteArrayInputStream(modifiedBytes));
        //addLoggerField(ctClass);
        if(formattedClassName.endsWith(methodName)) {
            for (CtConstructor constructor : ctClass.getConstructors()) {
                    String insertString = "com.asm.mja.logging.TraceFileLogger logger = com.asm.mja.logging.TraceFileLogger.getInstance(); try { " +
                            "com.asm.mja.utils.HeapDumpUtils.collectHeap();" +
                            "logger.trace(\"{" + formattedClassName + '.' + methodName + "} | " + event + " | " + "HEAP\"" + "); " +
                            "} catch (Exception e) {}";
                    if (event.equals(Event.INGRESS))
                        constructor.insertBefore(insertString);
                    else if (event.equals(Event.EGRESS))
                        constructor.insertAfter(insertString);
                    else
                        constructor.insertAt(lineNumber, insertString);
            }
        }
        else {
            for (CtMethod method : ctClass.getDeclaredMethods()) {
                if (method.getName().equals(methodName)) {
                    String insertString = "com.asm.mja.logging.TraceFileLogger logger = com.asm.mja.logging.TraceFileLogger.getInstance(); try { " +
                            "com.asm.mja.utils.HeapDumpUtils.collectHeap();" +
                            "logger.trace(\"{" + formattedClassName + '.' + methodName + "} | " + event + " | " + "HEAP\"" + "); " +
                            "} catch (Exception e) {}";
                    if (event.equals(Event.INGRESS))
                        method.insertBefore(insertString);
                    else if (event.equals(Event.EGRESS))
                        method.insertAfter(insertString);
                    else
                        method.insertAt(lineNumber, insertString);
                }
            }
        }
        // CtClass frozen - due to  writeFile()/toClass()/toBytecode()
        modifiedBytes = ctClass.toBytecode();

        // To remove from ClassPool
        ctClass.detach();
        return modifiedBytes;
    }

    /*
      For ref:
        $_ gives the return value
        $r gives the return type
     */
    private byte[] getReturnValue(String methodName, Event event, ClassLoader loader,
                                  String formattedClassName, Class<?> classBeingRedefined, byte[] modifiedBytes) throws IOException, CannotCompileException, UnsupportedActionException, NotFoundException {
        ClassPool pool = getClassPool();
        CtClass ctClass = pool.makeClass(new java.io.ByteArrayInputStream(modifiedBytes));
        //addLoggerField(ctClass);
        if(formattedClassName.endsWith(methodName)) {
            logger.warn("Constructors don't return values, please make sure you are not using RET for constructor instrumentation");
            return modifiedBytes;
        }
        for (CtMethod method : ctClass.getDeclaredMethods()) {
            if (method.getName().equals(methodName)) {
                StringBuilder code = new StringBuilder();
                CtClass returnType;
                try {
                    returnType = method.getReturnType();
                } catch (NotFoundException e) {
                    throw new UnsupportedActionException(e.getMessage());
                }

                if (event.equals(Event.EGRESS)) {
                    String returnVariableName;

                    if (returnType.equals(CtClass.voidType)) {
                        // For void methods, no return value to capture
                        code.append("com.asm.mja.logging.TraceFileLogger logger = com.asm.mja.logging.TraceFileLogger.getInstance();");
                        code.append("logger.trace(\"{").append(formattedClassName).append('.').append(methodName).append("} | ").append(event).append(" | RET | VOID\");");
                    } else if (returnType.isPrimitive()) {
                        // For primitive types, no need to call toString()
                        returnVariableName = "$$_returnValue";
                        code.append(returnType.getName()).append(' ').append(returnVariableName).append(" = ($r) $_;");
                        code.append("com.asm.mja.logging.TraceFileLogger logger = com.asm.mja.logging.TraceFileLogger.getInstance();");
                        code.append("try {");
                        code.append("    logger.trace(\"{").append(formattedClassName).append('.').append(methodName).append("} | ").append(event).append(" | RET | \" + ").append(returnVariableName).append(");");
                        code.append("} catch (Exception e) {}");
                    } else {
                        // For non-primitive types, check for null before calling toString()
                        returnVariableName = "$$_returnValue";
                        code.append(returnType.getName()).append(' ').append(returnVariableName).append(" = ($r) $_;");
                        code.append("com.asm.mja.logging.TraceFileLogger logger = com.asm.mja.logging.TraceFileLogger.getInstance();");
                        code.append("try {");
                        code.append("    if (").append(returnVariableName).append(" != null) {");
                        code.append("        logger.trace(\"{").append(formattedClassName).append('.').append(methodName).append("} | ").append(event).append(" | RET | \" + ").append(returnVariableName).append(".toString());");
                        code.append("    } else {");
                        code.append("        logger.trace(\"{").append(formattedClassName).append('.').append(methodName).append("} | ").append(event).append(" | RET | NULL\");");
                        code.append("    }");
                        code.append("} catch (Exception e) {}");
                    }
                } else {
                    throw new UnsupportedActionException("Getting return value for " + event + " is not supported");
                }

                method.insertAfter(code.toString()); // Insert after to capture return value
            }
        }
        // CtClass frozen - due to  writeFile()/toClass()/toBytecode()
        modifiedBytes = ctClass.toBytecode();

        // To remove from ClassPool
        ctClass.detach();
        return modifiedBytes;
    }

    private byte[] addCustomCode(String customCode, String methodName, Event event, ClassLoader loader,
                                 String formattedClassName, Class<?> classBeingRedefined, byte[] modifiedBytes, int lineNumber) throws IOException, CannotCompileException, NotFoundException {
        ClassPool pool = getClassPool();
        CtClass ctClass = pool.makeClass(new java.io.ByteArrayInputStream(modifiedBytes));
        if(formattedClassName.endsWith(methodName)) {
            for (CtConstructor constructor : ctClass.getConstructors()) {
                    String safeCustomCode = "try { " + customCode + " } catch (Exception e) { " +
                            "com.asm.mja.logging.TraceFileLogger logger = com.asm.mja.logging.TraceFileLogger.getInstance();" +
                            "logger.error(\"Custom code threw an exception in " + formattedClassName + '.' + methodName + ": \" + e.getMessage());" +
                            "}";
                    if(event.equals(Event.INGRESS))
                        constructor.insertBefore(safeCustomCode);
                    else if(event.equals(Event.CODEPOINT))
                        constructor.insertAt(lineNumber, safeCustomCode);
                    else
                        constructor.insertAfter(safeCustomCode);
                }
        }
        else {
            for (CtMethod method : ctClass.getDeclaredMethods()) {
                if (method.getName().equals(methodName)) {
                    String safeCustomCode = "try { " + customCode + " } catch (Exception e) { " +
                            "com.asm.mja.logging.TraceFileLogger logger = com.asm.mja.logging.TraceFileLogger.getInstance();" +
                            "logger.error(\"Custom code threw an exception in " + formattedClassName + '.' + methodName + ": \" + e.getMessage());" +
                            "}";
                    if(event.equals(Event.INGRESS))
                        method.insertBefore(safeCustomCode);
                    else if(event.equals(Event.CODEPOINT))
                        method.insertAt(lineNumber, safeCustomCode);
                    else
                        method.insertAfter(safeCustomCode);
                }
            }
        }
        // CtClass frozen - due to  writeFile()/toClass()/toBytecode()
        modifiedBytes = ctClass.toBytecode();

        // To remove from ClassPool
        ctClass.detach();
        return modifiedBytes;
    }
}
