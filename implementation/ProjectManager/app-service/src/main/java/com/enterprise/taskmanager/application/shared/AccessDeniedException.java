package com.enterprise.taskmanager.application.shared;

public final class AccessDeniedException extends ApplicationException {

    public AccessDeniedException() {
        super("The authenticated user is not a member of this organization");
    }
}
