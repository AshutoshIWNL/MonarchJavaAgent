package com.asm.mja.utils;

import com.asm.mja.rule.ReplacementSourceType;
import com.asm.mja.rule.Rule;
import com.asm.mja.transformer.Action;
import com.asm.mja.transformer.Event;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClassRuleUtilsTest {

    @Test
    void ruleClassesExcludesClassReplacementRules() {
        Class<?>[] loaded = new Class<?>[]{TargetA.class, TargetB.class};
        List<Rule> rules = Arrays.asList(
                new Rule(TargetA.class.getName(), "run", Event.INGRESS, Action.ARGS, 0),
                Rule.forClassReplacement(TargetB.class.getName(), ReplacementSourceType.FILE, "/tmp/TargetB.class")
        );

        Class<?>[] matched = ClassRuleUtils.ruleClasses(loaded, rules);
        assertEquals(1, matched.length);
        assertEquals(TargetA.class.getName(), matched[0].getName());
    }

    @Test
    void resolveRuleClassesSupportsWildcardPatterns() {
        Class<?>[] loaded = new Class<?>[]{TargetA.class, TargetB.class, String.class};
        List<Rule> rules = Collections.singletonList(
                Rule.forClassReplacement(
                        "com.asm.mja.utils.ClassRuleUtilsTest$Target*",
                        ReplacementSourceType.JAR,
                        "/tmp/patches.jar"
                )
        );

        List<Class<?>> matched = ClassRuleUtils.resolveRuleClasses(loaded, rules);
        List<String> names = matched.stream().map(Class::getName).collect(Collectors.toList());

        assertEquals(2, names.size());
        assertTrue(names.contains(TargetA.class.getName()));
        assertTrue(names.contains(TargetB.class.getName()));
    }

    static class TargetA {
        void run() {
        }
    }

    static class TargetB {
        void run() {
        }
    }
}
