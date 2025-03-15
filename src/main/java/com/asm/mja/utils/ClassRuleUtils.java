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
                .map(Rule::getClassName)
                .collect(Collectors.toSet());

        return Arrays.stream(allLoadedClasses)
                .filter(clazz -> classNamesToInstrument.contains(clazz.getName()))
                .toArray(Class<?>[]::new);
    }
}
