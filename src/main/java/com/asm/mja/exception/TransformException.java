package com.asm.mja.exception;

/**
 * @author ashut
 * @since 15-08-2024
 */

public class TransformException extends Exception{
    public TransformException() {
        super();
    }

    public TransformException(String message, Throwable t) {
        super(message, t);
    }

    public TransformException(String message) {
        super(message);
    }

    public TransformException(Throwable t) {
        super(t);
    }
}
