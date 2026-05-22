package com.asm.mja.transformer.handlers;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CustomCodeActionHandlerTest {

    @Test
    void rewriteMlogSimpleExpression() {
        String input = "MLOG(\"Money: \" + objA.getMoney());";
        String output = CustomCodeActionHandler.rewriteMlogMacros(input);
        assertEquals("com.asm.mja.logging.TraceFileLogger.getInstance().trace(String.valueOf(\"Money: \" + objA.getMoney()));", output);
    }

    @Test
    void rewriteMlogMultipleOccurrences() {
        String input = "int a = 10; MLOG(a); MLOG(\"b=\" + (a + 1));";
        String output = CustomCodeActionHandler.rewriteMlogMacros(input);
        assertEquals(
                "int a = 10; com.asm.mja.logging.TraceFileLogger.getInstance().trace(String.valueOf(a)); com.asm.mja.logging.TraceFileLogger.getInstance().trace(String.valueOf(\"b=\" + (a + 1)));",
                output
        );
    }

    @Test
    void rewriteMlogIgnoresNonMacroCode() {
        String input = "System.out.println(\"ok\");";
        String output = CustomCodeActionHandler.rewriteMlogMacros(input);
        assertEquals(input, output);
    }

    @Test
    void rewriteMlogRejectsUnbalancedParenthesis() {
        String input = "MLOG(\"x\" + value;";
        assertThrows(IllegalArgumentException.class, () -> CustomCodeActionHandler.rewriteMlogMacros(input));
    }

    @Test
    void rewriteMlogRejectsEmptyExpression() {
        String input = "MLOG(   );";
        assertThrows(IllegalArgumentException.class, () -> CustomCodeActionHandler.rewriteMlogMacros(input));
    }
}
