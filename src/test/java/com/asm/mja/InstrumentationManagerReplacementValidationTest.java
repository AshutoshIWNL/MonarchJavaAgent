package com.asm.mja;

import com.asm.mja.rule.ReplacementSourceType;
import com.asm.mja.rule.Rule;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarOutputStream;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class InstrumentationManagerReplacementValidationTest {

    @Test
    void validateReplacementSourceFailsWhenFilePathDoesNotExist() throws Exception {
        InstrumentationManager manager = new InstrumentationManager();
        Rule rule = Rule.forClassReplacement(
                DummyTarget.class.getName(),
                ReplacementSourceType.FILE,
                "C:\\does-not-exist\\DummyTarget.class"
        );

        IOException cause = invokeValidateReplacementSource(manager, rule, DummyTarget.class);
        assertTrue(cause.getMessage().contains("does not exist"));
    }

    @Test
    void validateReplacementSourceFailsWhenJarMissesTargetEntry() throws Exception {
        Path tempJar = Files.createTempFile("mja-empty", ".jar");
        try (JarOutputStream ignored = new JarOutputStream(Files.newOutputStream(tempJar))) {
            // create an empty jar
        }

        InstrumentationManager manager = new InstrumentationManager();
        Rule rule = Rule.forClassReplacement(
                DummyTarget.class.getName(),
                ReplacementSourceType.JAR,
                tempJar.toString()
        );

        IOException cause = invokeValidateReplacementSource(manager, rule, DummyTarget.class);
        assertTrue(cause.getMessage().contains("does not contain target entry"));
    }

    private IOException invokeValidateReplacementSource(InstrumentationManager manager, Rule rule, Class<?> targetClass)
            throws Exception {
        Method method = InstrumentationManager.class.getDeclaredMethod(
                "validateReplacementSource",
                Rule.class,
                Class.class
        );
        method.setAccessible(true);

        try {
            method.invoke(manager, rule, targetClass);
            fail("Expected replacement source validation to fail");
            return null;
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof IOException) {
                return (IOException) cause;
            }
            throw e;
        }
    }

    static class DummyTarget {
    }
}
