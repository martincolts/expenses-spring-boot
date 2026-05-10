package com.tincho.expenses.controllers;

import com.tincho.expenses.exceptions.CategoryNameAlreadyExistsException;
import com.tincho.expenses.exceptions.CategoryNotFoundException;
import com.tincho.expenses.exceptions.ExpenseAlreadyPaidException;
import com.tincho.expenses.exceptions.ExpenseNotFoundException;
import com.tincho.expenses.exceptions.ExpenseNotPaidException;
import com.tincho.expenses.exceptions.IncomeNotFoundException;
import com.tincho.expenses.exceptions.InvalidCurrentPasswordException;
import com.tincho.expenses.exceptions.TenantEmailAlreadyExistsException;
import com.tincho.expenses.exceptions.TenantNotFoundException;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
class ApiExceptionHandler {

    @ExceptionHandler(TenantNotFoundException.class)
    ProblemDetail notFound(TenantNotFoundException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(TenantEmailAlreadyExistsException.class)
    ProblemDetail conflict(TenantEmailAlreadyExistsException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(CategoryNotFoundException.class)
    ProblemDetail categoryNotFound(CategoryNotFoundException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(CategoryNameAlreadyExistsException.class)
    ProblemDetail categoryConflict(CategoryNameAlreadyExistsException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(ExpenseNotFoundException.class)
    ProblemDetail expenseNotFound(ExpenseNotFoundException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(IncomeNotFoundException.class)
    ProblemDetail incomeNotFound(IncomeNotFoundException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(ExpenseAlreadyPaidException.class)
    ProblemDetail expenseAlreadyPaid(ExpenseAlreadyPaidException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(ExpenseNotPaidException.class)
    ProblemDetail expenseNotPaid(ExpenseNotPaidException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail validation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, message);
    }

    @ExceptionHandler(BadCredentialsException.class)
    ProblemDetail badCredentials(BadCredentialsException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, "Invalid credentials");
    }

    @ExceptionHandler(InvalidCurrentPasswordException.class)
    ProblemDetail invalidCurrentPassword(InvalidCurrentPasswordException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(AccessDeniedException.class)
    ProblemDetail accessDenied(AccessDeniedException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, "Access denied");
    }
}
