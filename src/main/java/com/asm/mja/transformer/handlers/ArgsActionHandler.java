package com.asm.mja.transformer.handlers;

import com.asm.mja.exception.UnsupportedActionException;
import com.asm.mja.transformer.ActionExecution;
import com.asm.mja.transformer.Event;
import javassist.*;

import java.io.IOException;

/**
 * Handles ARGS action instrumentation.
 * @author ashut
 * @since 22-03-2026
 */
public class ArgsActionHandler extends AbstractActionHandler {

    public ArgsActionHandler(ClassPoolProvider classPoolProvider) {
        super(classPoolProvider);
    }

    @Override
    public byte[] apply(ActionExecution execution) throws IOException, CannotCompileException, UnsupportedActionException, NotFoundException {
        if (execution.getEvent().equals(Event.EGRESS)) {
            throw new UnsupportedActionException("Getting arguments for EGRESS is not supported");
        }
        if (execution.getEvent().equals(Event.CODEPOINT)) {
            throw new UnsupportedActionException("Getting arguments for CODEPOINT is not supported");
        }

        CtClass ctClass = toCtClass(execution.getModifiedBytes());
        applyToTargets(
                ctClass,
                execution.getFormattedClassName(),
                execution.getMethodName(),
                constructor -> constructor.insertBefore(buildArgsSnippet(constructor, execution.getFormattedClassName(), execution.getMethodName(), execution.getEvent())),
                method -> method.insertBefore(buildArgsSnippet(method, execution.getFormattedClassName(), execution.getMethodName(), execution.getEvent()))
        );
        return toBytecodeAndDetach(ctClass);
    }

    private String buildArgsSnippet(CtBehavior behavior, String formattedClassName, String methodName, Event event) {
        StringBuilder code = new StringBuilder();
        CtClass[] parameterTypes = new CtClass[0];
        try {
            parameterTypes = behavior.getParameterTypes();
        } catch (NotFoundException ignored) {
            // ignore and treat as no params
        }

        if (parameterTypes.length == 0) {
            code.append("try {");
            code.append("    com.asm.mja.logging.TraceFileLogger.getInstance().trace(\"{").append(formattedClassName).append('.').append(methodName).append("} | ").append(event).append(" | ARGS | NULL\");");
            code.append("} catch (Exception e) {}");
        } else {
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
            code.append("    com.asm.mja.logging.TraceFileLogger.getInstance().trace(\"{").append(formattedClassName).append('.').append(methodName).append("} | ").append(event).append(" | ARGS | \" + args.toString());");
            code.append("} catch (Exception e) {}");
        }
        return code.toString();
    }
}
