package com.asm.mja.rule;

/**
 * Represents a rejected rule and diagnostic details.
 */
public class RuleValidationIssue {
    private final String rule;
    private final String reason;
    private final String suggestion;

    public RuleValidationIssue(String rule, String reason, String suggestion) {
        this.rule = rule;
        this.reason = reason;
        this.suggestion = suggestion;
    }

    public String getRule() {
        return rule;
    }

    public String getReason() {
        return reason;
    }

    public String getSuggestion() {
        return suggestion;
    }
}
