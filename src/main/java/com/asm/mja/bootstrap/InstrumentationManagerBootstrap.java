package com.asm.mja.bootstrap;

import com.asm.mja.InstrumentationManager;
import com.asm.mja.config.Config;
import com.asm.mja.logging.TraceFileLogger;
import com.asm.mja.rule.Rule;
import com.asm.mja.transformer.GlobalTransformer;

import java.io.File;
import java.lang.instrument.Instrumentation;
import java.util.List;

/**
 * Bootstraps instrumentation manager wiring and startup.
 * @author ashut
 * @since 22-03-2026
 */
public class InstrumentationManagerBootstrap {

    private InstrumentationManagerBootstrap() {
    }

    public static void start(Instrumentation inst,
                             String configFile,
                             GlobalTransformer globalTransformer,
                             TraceFileLogger traceFileLogger,
                             List<Rule> rules,
                             Config config) {
        InstrumentationManager instrumentationManager = InstrumentationManager.getInstance();
        instrumentationManager.setInstrumentation(inst);
        instrumentationManager.setConfigFilePath(configFile);
        instrumentationManager.setInitialConfig(config);
        instrumentationManager.setTransformer(globalTransformer);
        instrumentationManager.setCurrentRules(rules);
        instrumentationManager.setLastModified(new File(configFile).lastModified());
        instrumentationManager.setLogger(traceFileLogger);
        instrumentationManager.setConfigRefreshInterval((long) config.getConfigRefreshInterval());
        instrumentationManager.execute();
    }
}
