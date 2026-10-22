package com.enterprise.taskmanager.application.shared;

public final class ResourceNotFoundException extends ApplicationException {

    public ResourceNotFoundException(String resourceName) {
        super(resourceName + " was not found");
    }
}
