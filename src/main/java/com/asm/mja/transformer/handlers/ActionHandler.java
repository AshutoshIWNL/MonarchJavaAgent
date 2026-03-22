package com.asm.mja.transformer.handlers;

import com.asm.mja.exception.UnsupportedActionException;
import com.asm.mja.transformer.ActionExecution;
import javassist.CannotCompileException;
import javassist.NotFoundException;

import java.io.IOException;

/**
 * Contract for bytecode action handlers.
 * @author ashut
 * @since 22-03-2026
 */
public interface ActionHandler {
    byte[] apply(ActionExecution execution) throws IOException, CannotCompileException, UnsupportedActionException, NotFoundException;
}
