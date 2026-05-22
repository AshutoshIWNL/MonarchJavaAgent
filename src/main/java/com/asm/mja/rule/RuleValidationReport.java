package com.asm.mja.rule;

import java.util.Collections;
import java.util.List;

/**
 * Rule validation report containing accepted and rejected rules.
 */
public class RuleValidationReport {
    private final List<Rule> acceptedRules;
    private final List<RuleValidationIssue> rejectedIssues;
    private final int skippedRules;

    public RuleValidationReport(List<Rule> acceptedRules, List<RuleValidationIssue> rejectedIssues, int skippedRules) {
        this.acceptedRules = acceptedRules;
        this.rejectedIssues = rejectedIssues;
        this.skippedRules = skippedRules;
    }

    public List<Rule> getAcceptedRules() {
        return Collections.unmodifiableList(acceptedRules);
    }

    public List<RuleValidationIssue> getRejectedIssues() {
        return Collections.unmodifiableList(rejectedIssues);
    }

    public int getSkippedRules() {
        return skippedRules;
    }
}
