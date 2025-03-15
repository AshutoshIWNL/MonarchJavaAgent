package com.asm.mja.exception;

/**
 * @author ashut
 * @since 15-08-2024
 */

public class BackupCreationException extends Exception{
    public BackupCreationException() {
        super();
    }

    public BackupCreationException(String message, Throwable t) {
        super(message, t);
    }

    public BackupCreationException(String message) {
        super(message);
    }

    public BackupCreationException(Throwable t) {
        super(t);
    }
}
