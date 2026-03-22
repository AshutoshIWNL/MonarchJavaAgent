package com.asm.mja.transformer.handlers;

import com.asm.mja.transformer.Event;
import javassist.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;

/**
 * Shared utilities for action handlers.
 * @author ashut
 * @since 22-03-2026
 */
public abstract class AbstractActionHandler implements ActionHandler {

    @FunctionalInterface
    protected interface ConstructorApplier {
        void apply(CtConstructor constructor) throws CannotCompileException, NotFoundException, IOException;
    }

    @FunctionalInterface
    protected interface MethodApplier {
        void apply(CtMethod method) throws CannotCompileException, NotFoundException, IOException;
    }

    protected final ClassPoolProvider classPoolProvider;

    protected AbstractActionHandler(ClassPoolProvider classPoolProvider) {
        this.classPoolProvider = classPoolProvider;
    }

    protected CtClass toCtClass(byte[] modifiedBytes) throws NotFoundException, IOException {
        ClassPool pool = classPoolProvider.getClassPool();
        return pool.makeClass(new ByteArrayInputStream(modifiedBytes));
    }

    protected byte[] toBytecodeAndDetach(CtClass ctClass) throws IOException, CannotCompileException {
        byte[] updated = ctClass.toBytecode();
        ctClass.detach();
        return updated;
    }

    protected boolean isConstructorTarget(String formattedClassName, String methodName) {
        return formattedClassName.endsWith(methodName);
    }

    protected void applyToTargets(CtClass ctClass,
                                  String formattedClassName,
                                  String methodName,
                                  ConstructorApplier constructorApplier,
                                  MethodApplier methodApplier) throws CannotCompileException, NotFoundException, IOException {
        if (isConstructorTarget(formattedClassName, methodName)) {
            for (CtConstructor constructor : ctClass.getConstructors()) {
                constructorApplier.apply(constructor);
            }
            return;
        }
        for (CtMethod method : ctClass.getDeclaredMethods()) {
            if (method.getName().equals(methodName)) {
                methodApplier.apply(method);
            }
        }
    }

    protected void insertForEvent(CtBehavior behavior, Event event, int lineNumber, String code) throws CannotCompileException {
        if (event.equals(Event.INGRESS)) {
            behavior.insertBefore(code);
        } else if (event.equals(Event.EGRESS)) {
            behavior.insertAfter(code);
        } else {
            behavior.insertAt(lineNumber, code);
        }
    }
}
