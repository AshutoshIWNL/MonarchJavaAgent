package com.asm.mja.rule;

import com.asm.mja.transformer.Action;
import com.asm.mja.transformer.Event;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author ashut
 * @since 20-04-2024
 */

public class RuleParser {

    private static final Pattern pattern = Pattern.compile("\\((\\d+)\\)");
    private static final Pattern addPattern = Pattern.compile("\\[([^]]+)\\]");
    private static final String monarchPackage = "com.asm.mja";

    public static List<Rule> parseRules(List<String> rules) {
        return rules.stream()
                .filter(rule -> !rule.startsWith(monarchPackage))
                .map(rule -> {
                    String[] parts = rule.split("::|@");

                    if (parts.length < 2) {
                        throw new IllegalArgumentException("Invalid rule format: " + rule);
                    }

                    String className = parts[0];
                    String secondToken = parts[1];
                    if ("CHANGE".equals(secondToken)) {
                        if (parts.length < 4) {
                            throw new IllegalArgumentException("Invalid CHANGE rule format: " + rule);
                        }
                        ReplacementSourceType sourceType = ReplacementSourceType.valueOf(parts[2]);
                        String sourcePath = extractBracketPayload(parts[3], "CHANGE source path");
                        return Rule.forClassReplacement(className, sourceType, sourcePath);
                    }

                    if (parts.length < 3) {
                        throw new IllegalArgumentException("Invalid rule format: " + rule);
                    }

                    String methodName = parts[1];
                    String eventString = parts[2];
                    Event event = null;
                    int lineNumber = 0;
                    if(eventString.startsWith("CODEPOINT")) {
                        event = Event.CODEPOINT;
                        Matcher matcher = pattern.matcher(eventString);
                        if (matcher.find()) {
                            lineNumber = Integer.parseInt(matcher.group(1));
                        }
                    } else if(eventString.startsWith("PROFILE")) {
                        event = Event.PROFILE;
                        return new Rule(className, methodName, event, null, lineNumber);
                    } else {
                        event = Event.valueOf(eventString);
                    }

                    if (parts.length < 4) {
                        throw new IllegalArgumentException("Invalid rule format: " + rule);
                    }

                    Action action = Action.valueOf(parts[3]);

                    String filterName=null;
                    if(action==Action.STACK && parts.length >4){
                        filterName = extractBracketPayload(parts[4], "STACK filter");
                    }
                    String customCode = null;
                    if (action == Action.ADD && parts.length > 4) {
                        customCode = extractBracketPayload(parts[4], "ADD custom code");
                    }

                    return new Rule(className, methodName, event, action, customCode, lineNumber ,filterName);
                })
                .collect(Collectors.toList());
    }

    private static String extractBracketPayload(String token, String context) {
        Matcher matcher = addPattern.matcher(token);
        if (matcher.find()) {
            return matcher.group(1);
        }
        throw new IllegalArgumentException("Expected bracketed payload for " + context + ": " + token);
    }

}
