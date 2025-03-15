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

                    String className = parts[0];
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
                    Action action = Action.valueOf(parts[3]);

                    String customCode = null;
                    if (action == Action.ADD && parts.length > 4) {
                        Matcher matcher = addPattern.matcher(parts[4]);
                        if (matcher.find()) {
                            customCode = matcher.group(1);
                        }
                    }

                    return new Rule(className, methodName, event, action, customCode, lineNumber);
                })
                .collect(Collectors.toList());
    }

}
