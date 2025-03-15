package com.asm.mja.exception;

/**
 * @author ashut
 * @since 20-04-2024
 */

public class UnsupportedActionException extends Exception{
    public UnsupportedActionException() {
        super();
    }

    public UnsupportedActionException(String message, Throwable t) {
        super(message, t);
    }

    public UnsupportedActionException(String message) {
        super(message);
    }

    public UnsupportedActionException(Throwable t) {
        super(t);
    }
}
