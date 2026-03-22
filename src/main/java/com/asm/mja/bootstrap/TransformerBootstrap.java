package com.asm.mja.bootstrap;

import com.asm.mja.config.Config;
import com.asm.mja.logging.AgentLogger;
import com.asm.mja.logging.TraceFileLogger;
import com.asm.mja.rule.Rule;
import com.asm.mja.transformer.GlobalTransformer;
import com.asm.mja.utils.ClassRuleUtils;

import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.util.Arrays;
import java.util.List;

/**
 * Bootstraps transformer creation and registration.
 * @author ashut
 * @since 22-03-2026
 */
public class TransformerBootstrap {

    private TransformerBootstrap() {
    }

    public static GlobalTransformer setupTransformer(Instrumentation inst,
                                                     Config config,
                                                     TraceFileLogger traceFileLogger,
                                                     List<Rule> rules,
                                                     String launchType,
                                                     String agentAbsolutePath) {
        GlobalTransformer globalTransformer = new GlobalTransformer(config, traceFileLogger, rules, launchType, agentAbsolutePath);

        AgentLogger.debug("Launch Type \"" + launchType + "\" detected");

        boolean retransformSupported = inst.isRetransformClassesSupported();
        if (retransformSupported) {
            Class<?>[] classesToInstrument = ClassRuleUtils.ruleClasses(inst.getAllLoadedClasses(), rules);
            inst.addTransformer(globalTransformer, Boolean.TRUE);

            try {
                AgentLogger.debug("Re-transforming classes: " + Arrays.toString(classesToInstrument));
                if (classesToInstrument.length > 0) {
                    inst.retransformClasses(classesToInstrument);
                }
            } catch (UnmodifiableClassException e) {
                AgentLogger.error("Error re-transforming classes: " + e.getMessage(), e);
            }
        } else {
            AgentLogger.debug("Retransform not supported, adding transformer for future class loads.");
            inst.addTransformer(globalTransformer, Boolean.FALSE);
        }
        AgentLogger.info("Registered transformer - " + GlobalTransformer.class);
        return globalTransformer;
    }
}
