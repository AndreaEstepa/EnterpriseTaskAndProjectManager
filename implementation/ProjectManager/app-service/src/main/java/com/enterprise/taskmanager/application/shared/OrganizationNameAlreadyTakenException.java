package com.enterprise.taskmanager.application.shared;

public final class OrganizationNameAlreadyTakenException extends ApplicationException {

    public OrganizationNameAlreadyTakenException() {
        super("An organization with this name already exists");
    }
}
