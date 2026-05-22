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
    private static final String MLOG_TOKEN = "MLOG(";

    public CustomCodeActionHandler(ClassPoolProvider classPoolProvider) {
        super(classPoolProvider);
    }

    @Override
    public byte[] apply(ActionExecution execution) throws IOException, CannotCompileException, NotFoundException {
        CtClass ctClass = toCtClass(execution.getModifiedBytes());
        String rewrittenCustomCode = rewriteMlogMacros(execution.getCustomCode());
        String safeCustomCode = "try { " + rewrittenCustomCode + " } catch (Exception e) { " +
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

    static String rewriteMlogMacros(String customCode) {
        if (customCode == null || customCode.isEmpty() || !customCode.contains(MLOG_TOKEN)) {
            return customCode;
        }

        StringBuilder out = new StringBuilder(customCode.length() + 64);
        int cursor = 0;
        while (cursor < customCode.length()) {
            int macroStart = customCode.indexOf(MLOG_TOKEN, cursor);
            if (macroStart < 0) {
                out.append(customCode.substring(cursor));
                break;
            }

            out.append(customCode, cursor, macroStart);
            int exprStart = macroStart + MLOG_TOKEN.length();
            int close = findMacroClose(customCode, exprStart);
            if (close < 0) {
                throw new IllegalArgumentException("Malformed MLOG macro: missing closing ')' in custom code: " + customCode);
            }

            String expr = customCode.substring(exprStart, close).trim();
            if (expr.isEmpty()) {
                throw new IllegalArgumentException("Malformed MLOG macro: expression is empty in custom code: " + customCode);
            }

            out.append("com.asm.mja.logging.TraceFileLogger.getInstance().trace(String.valueOf(")
                    .append(expr)
                    .append("))");

            cursor = close + 1;
        }
        return out.toString();
    }

    private static int findMacroClose(String source, int from) {
        int depth = 1;
        boolean inSingle = false;
        boolean inDouble = false;
        boolean escaped = false;
        for (int i = from; i < source.length(); i++) {
            char c = source.charAt(i);

            if (escaped) {
                escaped = false;
                continue;
            }
            if (c == '\\') {
                escaped = true;
                continue;
            }

            if (!inDouble && c == '\'') {
                inSingle = !inSingle;
                continue;
            }
            if (!inSingle && c == '\"') {
                inDouble = !inDouble;
                continue;
            }

            if (inSingle || inDouble) {
                continue;
            }

            if (c == '(') {
                depth++;
            } else if (c == ')') {
                depth--;
                if (depth == 0) {
                    return i;
                }
            }
        }
        return -1;
    }
}
