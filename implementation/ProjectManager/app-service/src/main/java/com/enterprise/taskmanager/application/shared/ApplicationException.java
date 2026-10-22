package com.enterprise.taskmanager.application.shared;

public abstract class ApplicationException extends RuntimeException {

    protected ApplicationException(String message) {
        super(message);
    }
}
