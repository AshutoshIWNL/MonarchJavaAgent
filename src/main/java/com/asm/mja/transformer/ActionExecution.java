package com.asm.mja.transformer;

/**
 * Carries rule execution context for action handlers.
 * @author ashut
 * @since 22-03-2026
 */
public class ActionExecution {
    private final String methodName;
    private final Event event;
    private final Action action;
    private final String customCode;
    private final String filterName;
    private final String formattedClassName;
    private final byte[] modifiedBytes;
    private final int lineNumber;

    public ActionExecution(String methodName,
                           Event event,
                           Action action,
                           String customCode,
                           String filterName,
                           String formattedClassName,
                           byte[] modifiedBytes,
                           int lineNumber) {
        this.methodName = methodName;
        this.event = event;
        this.action = action;
        this.customCode = customCode;
        this.filterName = filterName;
        this.formattedClassName = formattedClassName;
        this.modifiedBytes = modifiedBytes;
        this.lineNumber = lineNumber;
    }

    public String getMethodName() {
        return methodName;
    }

    public Event getEvent() {
        return event;
    }

    public Action getAction() {
        return action;
    }

    public String getCustomCode() {
        return customCode;
    }

    public String getFilterName() {
        return filterName;
    }

    public String getFormattedClassName() {
        return formattedClassName;
    }

    public byte[] getModifiedBytes() {
        return modifiedBytes;
    }

    public int getLineNumber() {
        return lineNumber;
    }
}
