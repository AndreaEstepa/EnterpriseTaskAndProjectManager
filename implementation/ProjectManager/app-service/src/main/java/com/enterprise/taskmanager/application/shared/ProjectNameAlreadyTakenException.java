package com.enterprise.taskmanager.application.shared;

public final class ProjectNameAlreadyTakenException extends ApplicationException {

    public ProjectNameAlreadyTakenException() {
        super("A project with this name already exists in the organization");
    }
}
