package com.asm.mja.config;

import com.asm.mja.logging.AgentLogger;
import com.asm.mja.rule.ReplacementSourceType;
import com.asm.mja.rule.Rule;
import com.asm.mja.rule.RuleParser;
import com.asm.mja.rule.RuleValidationIssue;
import com.asm.mja.rule.RuleValidationReport;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * The ConfigValidator class validates the configuration object.
 * It checks if the configuration is valid based on certain criteria.
 * If any of the criteria are not met, the validation fails.
 * @author ashut
 * @since 11-04-2024
 */

public class ConfigValidator {

    /**
     * Validates the given configuration object.
     *
     * @param config The configuration object to validate.
     * @return true if the configuration is valid, false otherwise.
     */
    public static boolean isValid(Config config) {
        return validate(config).isValid();
    }

    public static ConfigValidationResult validate(Config config) {
        AgentLogger.debug("Validating the config object");
        RuleValidationReport emptyRuleReport = new RuleValidationReport(
                Collections.<Rule>emptyList(),
                Collections.<RuleValidationIssue>emptyList(),
                0
        );

        if (config == null) {
            AgentLogger.error("Config object is null");
            return new ConfigValidationResult(false, emptyRuleReport);
        }

        RuleValidationReport ruleValidationReport = validateRules(config);
        logRuleValidationSummary(ruleValidationReport);

        if (!isInstrumentationValid(config, ruleValidationReport)) {
            return new ConfigValidationResult(false, ruleValidationReport);
        }

        if (!isObserverValid(config)) {
            return new ConfigValidationResult(false, ruleValidationReport);
        }

        if (!isAlertsValid(config)) {
            return new ConfigValidationResult(false, ruleValidationReport);
        }

        return new ConfigValidationResult(true, ruleValidationReport);
    }

    private static boolean isInstrumentationValid(Config config, RuleValidationReport ruleValidationReport) {
        if (!config.isInstrumentationActive()) {
            return true;
        }

        String traceLocation = config.getTraceFileLocation();
        if (traceLocation == null || traceLocation.isEmpty() || !new File(traceLocation).isDirectory()) {
            AgentLogger.error("trace file directory doesn't exist or is not a directory");
            return false;
        }

        if (config.getAgentRules() == null || config.getAgentRules().isEmpty()) {
            AgentLogger.error("Rules are missing or empty");
            return false;
        }

        return true;
    }

    private static boolean isObserverValid(Config config) {
        if (!config.isObserverActive()) {
            return true;
        }

        if (config.isExposeMetrics() && config.getMetricsPort() <= 0) {
            AgentLogger.error("Metrics port must be greater than zero when metrics exposure is enabled");
            return false;
        }

        return true;
    }

    private static boolean isAlertsValid(Config config) {
        if (!config.isAlertsActive()) {
            return true;
        }

        if (config.getMaxHeapDumps() < 0) {
            AgentLogger.error("Max heap dumps cannot be negative");
            return false;
        }

        return true;
    }

    private static RuleValidationReport validateRules(Config config) {
        if (config == null || config.getAgentRules() == null || config.getAgentRules().isEmpty()) {
            return new RuleValidationReport(
                    Collections.<Rule>emptyList(),
                    Collections.<RuleValidationIssue>emptyList(),
                    0
            );
        }

        List<String> rawRules = new ArrayList<String>(config.getAgentRules());
        RuleValidationReport parseReport = RuleParser.parseRulesWithDiagnostics(rawRules);
        List<RuleValidationIssue> issues = new ArrayList<RuleValidationIssue>(parseReport.getRejectedIssues());

        for (Rule acceptedRule : parseReport.getAcceptedRules()) {
            if (!acceptedRule.isClassReplacementRule()) {
                continue;
            }
            RuleValidationIssue issue = validateReplacementRule(acceptedRule);
            if (issue != null) {
                issues.add(issue);
            }
        }

        return new RuleValidationReport(parseReport.getAcceptedRules(), issues, parseReport.getSkippedRules());
    }

    private static RuleValidationIssue validateReplacementRule(Rule rule) {
        String sourcePath = rule.getReplacementSourcePath();
        if (sourcePath == null || sourcePath.trim().isEmpty()) {
            return new RuleValidationIssue(
                    formatRule(rule),
                    "CHANGE source path is empty",
                    "Provide a valid absolute path inside brackets, e.g. ::FILE::[/opt/patch/MyClass.class]"
            );
        }

        File source = new File(sourcePath);
        if (!source.exists()) {
            return new RuleValidationIssue(formatRule(rule), "Replacement source path does not exist: " + sourcePath, "Create the file/jar or fix the configured path.");
        }
        if (!source.isFile()) {
            return new RuleValidationIssue(formatRule(rule), "Replacement source path is not a file: " + sourcePath, "Point to a regular file path.");
        }
        if (!source.canRead()) {
            return new RuleValidationIssue(formatRule(rule), "Replacement source path is not readable: " + sourcePath, "Fix file permissions for the agent process.");
        }

        if (rule.getReplacementSourceType() == ReplacementSourceType.FILE) {
            if (!source.getName().endsWith(".class")) {
                return new RuleValidationIssue(formatRule(rule), "FILE replacement must target a .class file: " + sourcePath, "Use CHANGE::FILE with a .class file path.");
            }
            if (!rule.getClassName().endsWith(".*")) {
                String expected = rule.getClassName().substring(rule.getClassName().lastIndexOf('.') + 1) + ".class";
                if (!expected.equals(source.getName())) {
                    return new RuleValidationIssue(
                            formatRule(rule),
                            "Replacement class file name mismatch. Expected " + expected + " but found " + source.getName(),
                            "Use a matching .class file for the configured class pattern."
                    );
                }
            }
        } else if (rule.getReplacementSourceType() == ReplacementSourceType.JAR) {
            if (!source.getName().endsWith(".jar")) {
                return new RuleValidationIssue(formatRule(rule), "JAR replacement must target a .jar file: " + sourcePath, "Use CHANGE::JAR with a jar file path.");
            }
            if (!rule.getClassName().endsWith(".*")) {
                String entry = rule.getClassName().replace('.', '/') + ".class";
                if (!jarContainsEntry(source, entry)) {
                    return new RuleValidationIssue(
                            formatRule(rule),
                            "Replacement jar does not contain target entry " + entry,
                            "Build or provide a jar containing the target class entry."
                    );
                }
            }
        }

        return null;
    }

    private static boolean jarContainsEntry(File jarFile, String entryName) {
        try (JarFile jf = new JarFile(jarFile)) {
            Enumeration<JarEntry> entries = jf.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (entryName.equals(entry.getName())) {
                    return true;
                }
            }
        } catch (IOException e) {
            return false;
        }
        return false;
    }

    private static String formatRule(Rule rule) {
        if (rule == null) {
            return "<unknown-rule>";
        }
        if (rule.isClassReplacementRule()) {
            return rule.getClassName()
                    + "@CHANGE::"
                    + rule.getReplacementSourceType()
                    + "::["
                    + rule.getReplacementSourcePath()
                    + "]";
        }
        return rule.toString();
    }

    private static void logRuleValidationSummary(RuleValidationReport report) {
        AgentLogger.info("Rule validation summary: accepted=" + report.getAcceptedRules().size()
                + ", rejected=" + report.getRejectedIssues().size()
                + ", skipped=" + report.getSkippedRules());
        if (!report.getRejectedIssues().isEmpty()) {
            AgentLogger.warning("Some rules were rejected during validation. Agent will continue with accepted rules.");
        }
        for (RuleValidationIssue issue : report.getRejectedIssues()) {
            AgentLogger.error("Rejected rule: " + issue.getRule() + "; reason=" + issue.getReason()
                    + "; suggestion=" + issue.getSuggestion());
        }
    }
}
