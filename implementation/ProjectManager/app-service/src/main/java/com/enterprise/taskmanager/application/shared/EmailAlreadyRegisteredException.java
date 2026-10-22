package com.enterprise.taskmanager.application.shared;

public final class EmailAlreadyRegisteredException extends ApplicationException {

    public EmailAlreadyRegisteredException() {
        super("An account with this email already exists");
    }
}
