package com.asm.mja.transformer;

import com.asm.mja.config.Config;
import com.asm.mja.logging.TraceFileLogger;
import com.asm.mja.rule.Rule;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class GlobalTransformerSafetyTest {

    @Test
    void transformReturnsOriginalBytecodeOnUnexpectedThrowable() throws Exception {
        Path traceDir = Files.createTempDirectory("mja-transform-safety");
        TraceFileLogger logger = TraceFileLogger.getInstance();
        logger.init(traceDir.toString());

        Config config = new Config();
        config.setPrintClassLoaderTrace(false);
        GlobalTransformer transformer = new GlobalTransformer(
                config,
                logger,
                Collections.<Rule>emptyList(),
                "javaagent",
                null
        );

        transformer.resetConfig(null);
        byte[] original = new byte[]{1, 2, 3, 4};

        byte[] transformed = assertDoesNotThrow(() ->
                transformer.transform(null, "com/example/TestClass", null, null, original)
        );

        assertArrayEquals(original, transformed);
        logger.close();
    }
}
