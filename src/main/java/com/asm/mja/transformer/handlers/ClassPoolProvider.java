package com.asm.mja.transformer.handlers;

import javassist.ClassPool;
import javassist.NotFoundException;

/**
 * Supplies ClassPool instances to transformer action handlers.
 * @author ashut
 * @since 22-03-2026
 */
@FunctionalInterface
public interface ClassPoolProvider {
    ClassPool getClassPool() throws NotFoundException;
}
