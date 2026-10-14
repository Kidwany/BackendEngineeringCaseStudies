package com.casestudies.payoutdelivery.fakebank.bank.adapter.in.api.error;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.casestudies.payoutdelivery.fakebank.bank.domain.exception.SimulatedBankFailureException;
import com.casestudies.payoutdelivery.fakebank.bank.domain.exception.SimulatedBankTimeoutException;

@Slf4j
@RestControllerAdvice
class ApiExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail onValidationFailure(MethodArgumentNotValidException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, "Request validation failed.");
        problem.setTitle("Invalid request");
        problem.setType(URI.create("urn:problem:validation-failed"));
        problem.setProperty("errors", fieldErrors(ex));
        return problem;
    }

    @ExceptionHandler({MissingRequestHeaderException.class, HttpMessageNotReadableException.class})
    ProblemDetail onMalformedRequest(Exception ex) {
        return badRequest(ex instanceof MissingRequestHeaderException missing
                ? "Header " + missing.getHeaderName() + " is required."
                : "Request body could not be read.");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail onRejectedValue(IllegalArgumentException ex) {
        return badRequest(ex.getMessage());
    }

    private ProblemDetail badRequest(String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
        problem.setTitle("Invalid request");
        problem.setType(URI.create("urn:problem:invalid-request"));
        return problem;
    }

    @ExceptionHandler(SimulatedBankFailureException.class)
    ProblemDetail onSimulatedFailure(SimulatedBankFailureException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());
        problem.setTitle("Bank failure");
        problem.setType(URI.create("urn:problem:simulated-bank-failure"));
        return problem;
    }

    @ExceptionHandler(SimulatedBankTimeoutException.class)
    ProblemDetail onSimulatedTimeout(SimulatedBankTimeoutException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.GATEWAY_TIMEOUT, ex.getMessage());
        problem.setTitle("Bank timeout");
        problem.setType(URI.create("urn:problem:simulated-bank-timeout"));
        return problem;
    }

    @ExceptionHandler(Exception.class)
    ProblemDetail onUnexpectedFailure(Exception ex) {
        if (ex instanceof ErrorResponse springError) {
            return springError.getBody();
        }
        log.error("Unexpected failure", ex);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR, "The request could not be processed.");
        problem.setTitle("Internal error");
        problem.setType(URI.create("urn:problem:internal-error"));
        return problem;
    }

    private Map<String, String> fieldErrors(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new LinkedHashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errors.putIfAbsent(error.getField(), error.getDefaultMessage());
        }
        return errors;
    }
}
