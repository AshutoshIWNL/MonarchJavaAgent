package com.asm.mja.transformer.handlers;

import com.asm.mja.logging.AgentLogger;
import com.asm.mja.transformer.ActionExecution;
import com.asm.mja.transformer.Event;
import javassist.CannotCompileException;
import javassist.CtClass;
import javassist.NotFoundException;

import java.io.IOException;

/**
 * Handles STACK action instrumentation.
 * @author ashut
 * @since 22-03-2026
 */
public class StackActionHandler extends AbstractActionHandler {

    public StackActionHandler(ClassPoolProvider classPoolProvider) {
        super(classPoolProvider);
    }

    @Override
    public byte[] apply(ActionExecution execution) throws IOException, CannotCompileException, NotFoundException {
        CtClass ctClass = toCtClass(execution.getModifiedBytes());
        String insertString = buildStackSnippet(
                execution.getFormattedClassName(),
                execution.getMethodName(),
                execution.getEvent(),
                execution.getFilterName()
        );

        applyToTargets(
                ctClass,
                execution.getFormattedClassName(),
                execution.getMethodName(),
                constructor -> insertForEvent(constructor, execution.getEvent(), execution.getLineNumber(), insertString),
                method -> {
                    if (execution.getEvent().equals(Event.INGRESS)) {
                        AgentLogger.info("Insert Before Code:" + insertString);
                    }
                    insertForEvent(method, execution.getEvent(), execution.getLineNumber(), insertString);
                }
        );
        return toBytecodeAndDetach(ctClass);
    }

    private String buildStackSnippet(String formattedClassName, String methodName, Event event, String filterName) {
        StringBuilder code = new StringBuilder();
        code.append("{\n");
        code.append("  try {\n");
        code.append("    StackTraceElement[] stack = new Throwable().getStackTrace();\n");

        if (filterName != null && !filterName.isEmpty()) {
            code.append("    boolean shouldLog = false;\n");
            code.append("    for (int i = 0; i < stack.length; i++) {\n");
            code.append("      StackTraceElement element = stack[i];\n");
            code.append("      String signature = element.getClassName() + \".\" + element.getMethodName();\n");
            code.append("      if (signature.indexOf(\"").append(filterName).append("\") >= 0) {\n");
            code.append("        shouldLog = true;\n");
            code.append("        break;\n");
            code.append("      }\n");
            code.append("    }\n");
            code.append("    if (shouldLog) {\n");
            code.append("      com.asm.mja.logging.TraceFileLogger.getInstance()\n");
            code.append("        .stack(\"{").append(formattedClassName).append(".")
                    .append(methodName).append("} | ").append(event)
                    .append(" | STACK\", stack);\n");
            code.append("    }\n");
        } else {
            code.append("    com.asm.mja.logging.TraceFileLogger.getInstance()\n");
            code.append("      .stack(\"{").append(formattedClassName).append(".")
                    .append(methodName).append("} | ").append(event)
                    .append(" | STACK\", stack);\n");
        }

        code.append("  } catch (Exception e) {\n");
        code.append("    e.printStackTrace();\n");
        code.append("  }\n");
        code.append("}\n");
        return code.toString();
    }
}
