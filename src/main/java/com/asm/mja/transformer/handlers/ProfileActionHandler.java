package com.asm.mja.transformer.handlers;

import com.asm.mja.transformer.ActionExecution;
import javassist.CannotCompileException;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.NotFoundException;

import java.io.IOException;

/**
 * Handles PROFILE event instrumentation.
 * @author ashut
 * @since 22-03-2026
 */
public class ProfileActionHandler extends AbstractActionHandler {

    public ProfileActionHandler(ClassPoolProvider classPoolProvider) {
        super(classPoolProvider);
    }

    @Override
    public byte[] apply(ActionExecution execution) throws IOException, CannotCompileException, NotFoundException {
        CtClass ctClass = toCtClass(execution.getModifiedBytes());
        for (CtMethod method : ctClass.getDeclaredMethods()) {
            if (method.getName().equals(execution.getMethodName())) {
                method.addLocalVariable("startTime", CtClass.longType);
                method.insertBefore("try { startTime = System.nanoTime(); } catch(Exception e){}");
                method.insertAfter("try {" +
                        "    long endTime = System.nanoTime();" +
                        "    final long executionTime = (endTime - startTime) / 1000000;" +
                        "    com.asm.mja.logging.TraceFileLogger.getInstance().trace(\"{" + execution.getFormattedClassName() + '.' + execution.getMethodName() + "} | PROFILE | Execution time: \" + executionTime + \"ms\");" +
                        "} catch (Exception e) { }");
            }
        }
        return toBytecodeAndDetach(ctClass);
    }
}
