package com.asm.mja.transformer.handlers;

import com.asm.mja.transformer.ActionExecution;
import javassist.CannotCompileException;
import javassist.CtClass;
import javassist.NotFoundException;

import java.io.IOException;

/**
 * Handles HEAP action instrumentation.
 * @author ashut
 * @since 22-03-2026
 */
public class HeapActionHandler extends AbstractActionHandler {

    public HeapActionHandler(ClassPoolProvider classPoolProvider) {
        super(classPoolProvider);
    }

    @Override
    public byte[] apply(ActionExecution execution) throws IOException, CannotCompileException, NotFoundException {
        CtClass ctClass = toCtClass(execution.getModifiedBytes());
        String insertString = "try { " +
                "com.asm.mja.utils.HeapDumpUtils.collectHeap();" +
                "com.asm.mja.logging.TraceFileLogger.getInstance().trace(\"{" + execution.getFormattedClassName() + '.' + execution.getMethodName() + "} | " + execution.getEvent() + " | HEAP\"); " +
                "} catch (Exception e) {}";

        applyToTargets(
                ctClass,
                execution.getFormattedClassName(),
                execution.getMethodName(),
                constructor -> insertForEvent(constructor, execution.getEvent(), execution.getLineNumber(), insertString),
                method -> insertForEvent(method, execution.getEvent(), execution.getLineNumber(), insertString)
        );
        return toBytecodeAndDetach(ctClass);
    }
}
