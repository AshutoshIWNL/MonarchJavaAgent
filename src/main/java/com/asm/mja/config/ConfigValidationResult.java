package com.asm.mja.config;

import com.asm.mja.rule.RuleValidationReport;

/**
 * Structured config validation outcome with rule-level diagnostics.
 */
public class ConfigValidationResult {
    private final boolean valid;
    private final RuleValidationReport ruleValidationReport;

    public ConfigValidationResult(boolean valid, RuleValidationReport ruleValidationReport) {
        this.valid = valid;
        this.ruleValidationReport = ruleValidationReport;
    }

    public boolean isValid() {
        return valid;
    }

    public RuleValidationReport getRuleValidationReport() {
        return ruleValidationReport;
    }
}
