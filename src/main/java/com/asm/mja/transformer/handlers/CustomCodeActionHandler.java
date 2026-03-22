package com.asm.mja.transformer.handlers;

import com.asm.mja.transformer.ActionExecution;
import javassist.CannotCompileException;
import javassist.CtClass;
import javassist.NotFoundException;

import java.io.IOException;

/**
 * Handles ADD action instrumentation.
 * @author ashut
 * @since 22-03-2026
 */
public class CustomCodeActionHandler extends AbstractActionHandler {

    public CustomCodeActionHandler(ClassPoolProvider classPoolProvider) {
        super(classPoolProvider);
    }

    @Override
    public byte[] apply(ActionExecution execution) throws IOException, CannotCompileException, NotFoundException {
        CtClass ctClass = toCtClass(execution.getModifiedBytes());
        String safeCustomCode = "try { " + execution.getCustomCode() + " } catch (Exception e) { " +
                "com.asm.mja.logging.TraceFileLogger.getInstance().error(\"Custom code threw an exception in " + execution.getFormattedClassName() + '.' + execution.getMethodName() + ": \" + e.getMessage());" +
                "}";

        applyToTargets(
                ctClass,
                execution.getFormattedClassName(),
                execution.getMethodName(),
                constructor -> insertForEvent(constructor, execution.getEvent(), execution.getLineNumber(), safeCustomCode),
                method -> insertForEvent(method, execution.getEvent(), execution.getLineNumber(), safeCustomCode)
        );
        return toBytecodeAndDetach(ctClass);
    }
}
