package com.enterprise.taskmanager.application.shared;

public final class InvalidCredentialsException extends ApplicationException {

    public InvalidCredentialsException() {
        super("Email or password is incorrect");
    }
}
