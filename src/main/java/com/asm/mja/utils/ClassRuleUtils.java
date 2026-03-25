package com.asm.mja.utils;

import com.asm.mja.rule.Rule;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author ashut
 * @since 15-08-2024
 */

public class ClassRuleUtils {
    public static Class<?>[] ruleClasses(Class<?>[] allLoadedClasses, List<Rule> rules) {
        Set<String> classNamesToInstrument = rules.stream()
                .filter(rule -> !rule.isClassReplacementRule())
                .map(Rule::getClassName)
                .collect(Collectors.toSet());

        return Arrays.stream(allLoadedClasses)
                .filter(clazz -> classNamesToInstrument.contains(clazz.getName()))
                .toArray(Class<?>[]::new);
    }

    public static List<Class<?>> resolveRuleClasses(Class<?>[] allLoadedClasses, List<Rule> rules) {
        return Arrays.stream(allLoadedClasses)
                .filter(clazz -> rules.stream().anyMatch(rule -> matchesClassPattern(rule.getClassName(), clazz.getName())))
                .collect(Collectors.toList());
    }

    private static boolean matchesClassPattern(String classPattern, String className) {
        if (classPattern == null || className == null) {
            return false;
        }
        if (classPattern.endsWith("*")) {
            String prefix = classPattern.substring(0, classPattern.length() - 1);
            return className.startsWith(prefix);
        }
        return className.equals(classPattern);
    }
}
