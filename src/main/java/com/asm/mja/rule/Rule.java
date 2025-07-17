package com.asm.mja.rule;

import com.asm.mja.transformer.Action;
import com.asm.mja.transformer.Event;

/**
 * @author ashut
 * @since 20-04-2024
 */

public class Rule {
    private String className;
    private String methodName;
    private Event event;
    private Action action;

    private String customCode;
    private int lineNumber;

    private String filterName;
    public Rule(String className, String methodName, Event event, Action action, int lineNumber) {
        this(className, methodName, event, action, null, lineNumber);
    }

    public Rule(String className, String methodName, Event event, Action action, String customCode, int lineNumber) {
        this.className = className;
        this.methodName = methodName;
        this.event = event;
        this.action = action;
        this.lineNumber = lineNumber;
        this.customCode = customCode;
    }
    public Rule(String className, String methodName, Event event, Action action, String customCode, int lineNumber,String filterName) {
        this.className = className;
        this.methodName = methodName;
        this.event = event;
        this.action = action;
        this.lineNumber = lineNumber;
        this.customCode = customCode;
        this.filterName=filterName;
    }


    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public Event getEvent() {
        return event;
    }

    public void setEvent(Event event) {
        this.event = event;
    }

    public Action getAction() {
        return action;
    }

    public void setAction(Action action) {
        this.action = action;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public void setLineNumber(int lineNumber) {
        this.lineNumber = lineNumber;
    }

    public String getCustomCode() {
        return customCode;
    }

    public void setCustomCode(String customCode) {
        this.customCode = customCode;
    }

    public void setFilterName(String filterName){
        this.filterName=filterName;
    }
    public String getFilterName(){
        return filterName;
    }
}
