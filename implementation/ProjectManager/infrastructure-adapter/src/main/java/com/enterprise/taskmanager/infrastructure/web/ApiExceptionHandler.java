package com.enterprise.taskmanager.infrastructure.web;

import com.enterprise.taskmanager.application.shared.AccessDeniedException;
import com.enterprise.taskmanager.application.shared.EmailAlreadyRegisteredException;
import com.enterprise.taskmanager.application.shared.InvalidCredentialsException;
import com.enterprise.taskmanager.application.shared.OrganizationNameAlreadyTakenException;
import com.enterprise.taskmanager.application.shared.ResourceNotFoundException;
import com.enterprise.taskmanager.domain.shared.DomainValidationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler({DomainValidationException.class, MethodArgumentNotValidException.class})
    ResponseEntity<ProblemDetail> handleBadRequest(Exception exception) {
        return problem(HttpStatus.BAD_REQUEST, exception.getMessage());
    }

    @ExceptionHandler({EmailAlreadyRegisteredException.class, OrganizationNameAlreadyTakenException.class})
    ResponseEntity<ProblemDetail> handleConflict(Exception exception) {
        return problem(HttpStatus.CONFLICT, exception.getMessage());
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    ResponseEntity<ProblemDetail> handleInvalidCredentials(InvalidCredentialsException exception) {
        return problem(HttpStatus.UNAUTHORIZED, exception.getMessage());
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    ResponseEntity<ProblemDetail> handleNotFound(ResourceNotFoundException exception) {
        return problem(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ProblemDetail> handleForbidden(AccessDeniedException exception) {
        return problem(HttpStatus.FORBIDDEN, exception.getMessage());
    }

    private static ResponseEntity<ProblemDetail> problem(HttpStatus status, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(status.getReasonPhrase());
        problem.setProperty("timestamp", Instant.now());
        return ResponseEntity.status(status).body(problem);
    }
}
