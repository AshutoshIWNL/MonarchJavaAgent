package com.asm.mja.rule;

import com.asm.mja.transformer.Event;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RuleParserTest {

    @Test
    void parseSupportsChangeFileRule() {
        List<Rule> parsed = RuleParser.parseRules(Arrays.asList(
                "com.asm.test.ClassA@CHANGE::FILE::[/root/files/ClassA.class]"
        ));

        assertEquals(1, parsed.size());
        Rule rule = parsed.get(0);
        assertTrue(rule.isClassReplacementRule());
        assertEquals(Event.CHANGE, rule.getEvent());
        assertEquals("com.asm.test.ClassA", rule.getClassName());
        assertEquals(ReplacementSourceType.FILE, rule.getReplacementSourceType());
        assertEquals("/root/files/ClassA.class", rule.getReplacementSourcePath());
    }

    @Test
    void parseSupportsChangeJarRule() {
        List<Rule> parsed = RuleParser.parseRules(Arrays.asList(
                "com.asm.test.*@CHANGE::JAR::[/root/files/patches.jar]"
        ));

        assertEquals(1, parsed.size());
        Rule rule = parsed.get(0);
        assertTrue(rule.isClassReplacementRule());
        assertEquals("com.asm.test.*", rule.getClassName());
        assertEquals(ReplacementSourceType.JAR, rule.getReplacementSourceType());
        assertEquals("/root/files/patches.jar", rule.getReplacementSourcePath());
    }

    @Test
    void parseRejectsMalformedChangeRule() {
        assertThrows(IllegalArgumentException.class, () -> RuleParser.parseRules(Arrays.asList(
                "com.asm.test.ClassA@CHANGE::FILE::/root/files/ClassA.class"
        )));
    }
}
