package com.asm.mja.transformer.handlers;

import com.asm.mja.exception.UnsupportedActionException;
import com.asm.mja.logging.TraceFileLogger;
import com.asm.mja.transformer.ActionExecution;
import com.asm.mja.transformer.Event;
import javassist.*;

import java.io.IOException;

/**
 * Handles RET action instrumentation.
 * @author ashut
 * @since 22-03-2026
 */
public class ReturnActionHandler extends AbstractActionHandler {

    private final TraceFileLogger logger;

    public ReturnActionHandler(ClassPoolProvider classPoolProvider, TraceFileLogger logger) {
        super(classPoolProvider);
        this.logger = logger;
    }

    @Override
    public byte[] apply(ActionExecution execution) throws IOException, CannotCompileException, UnsupportedActionException, NotFoundException {
        if (!execution.getEvent().equals(Event.EGRESS)) {
            throw new UnsupportedActionException("Getting return value for " + execution.getEvent() + " is not supported");
        }

        if (isConstructorTarget(execution.getFormattedClassName(), execution.getMethodName())) {
            logger.warn("Constructors don't return values, please make sure you are not using RET for constructor instrumentation");
            return execution.getModifiedBytes();
        }

        CtClass ctClass = toCtClass(execution.getModifiedBytes());
        for (CtMethod method : ctClass.getDeclaredMethods()) {
            if (method.getName().equals(execution.getMethodName())) {
                method.insertAfter(buildReturnSnippet(method, execution.getFormattedClassName(), execution.getMethodName(), execution.getEvent()));
            }
        }
        return toBytecodeAndDetach(ctClass);
    }

    private String buildReturnSnippet(CtMethod method, String formattedClassName, String methodName, Event event) throws UnsupportedActionException {
        StringBuilder code = new StringBuilder();
        CtClass returnType;
        try {
            returnType = method.getReturnType();
        } catch (NotFoundException e) {
            throw new UnsupportedActionException(e.getMessage());
        }

        if (returnType.equals(CtClass.voidType)) {
            code.append("com.asm.mja.logging.TraceFileLogger.getInstance().trace(\"{").append(formattedClassName).append('.').append(methodName).append("} | ").append(event).append(" | RET | VOID\");");
        } else if (returnType.isPrimitive()) {
            String returnVariableName = "$$_returnValue";
            code.append(returnType.getName()).append(' ').append(returnVariableName).append(" = ($r) $_;");
            code.append("try {");
            code.append("    com.asm.mja.logging.TraceFileLogger.getInstance().trace(\"{").append(formattedClassName).append('.').append(methodName).append("} | ").append(event).append(" | RET | \" + ").append(returnVariableName).append(");");
            code.append("} catch (Exception e) {}");
        } else {
            String returnVariableName = "$$_returnValue";
            code.append(returnType.getName()).append(' ').append(returnVariableName).append(" = ($r) $_;");
            code.append("try {");
            code.append("    if (").append(returnVariableName).append(" != null) {");
            code.append("        com.asm.mja.logging.TraceFileLogger.getInstance().trace(\"{").append(formattedClassName).append('.').append(methodName).append("} | ").append(event).append(" | RET | \" + ").append(returnVariableName).append(".toString());");
            code.append("    } else {");
            code.append("        com.asm.mja.logging.TraceFileLogger.getInstance().trace(\"{").append(formattedClassName).append('.').append(methodName).append("} | ").append(event).append(" | RET | NULL\");");
            code.append("    }");
            code.append("} catch (Exception e) {}");
        }
        return code.toString();
    }
}
