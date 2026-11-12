package com.leo.pos;

import java.sql.SQLException;
import java.time.Instant;

import org.hibernate.TransientPropertyValueException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import jakarta.persistence.EntityNotFoundException;

@ControllerAdvice
public class GlobalExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<ApiResponse<String>> handleJsonError(HttpMessageNotReadableException ex){
		return ResponseEntity.status(400).body(new ApiResponse<>(null, "Failed to parse request payload! Check all fields and format.", false, Instant.now())); 
	}
	
	@ExceptionHandler(SQLException.class)
	public ResponseEntity<ApiResponse<String>> handleSqlError(SQLException ex){ 
		log.error("SQL Exception encountered: ", ex);
		return ResponseEntity.status(500).body(new ApiResponse<>(null,
				"A database operation failed. Please try again.", false, Instant.now())); 
	}

	@ExceptionHandler(org.springframework.dao.DataAccessException.class)
	public ResponseEntity<ApiResponse<String>> handleDataAccessException(org.springframework.dao.DataAccessException ex){ 
		log.error("Data access exception: ", ex);
		return ResponseEntity.status(500).body(new ApiResponse<>(null,
				"A database operation failed. Please try again.", false, Instant.now())); 
	}
	
	@ExceptionHandler(MethodArgumentNotValidException.class) 
	public ResponseEntity<ApiResponse<String>> handleInvalidData(MethodArgumentNotValidException ex){ 
		String message = ex.getBindingResult().getFieldError() != null ? ex.getBindingResult().getFieldError().getDefaultMessage() : "Invalid input data.";
		return ResponseEntity.status(400).body(new ApiResponse<>(null, message, false, Instant.now()));   
	}

	@ExceptionHandler(NullPointerException.class)
	public ResponseEntity<ApiResponse<String>> handleNullPointer(NullPointerException ex){ 
		log.error("NullPointerException occurred: ", ex);
		return ResponseEntity.status(400).body(new ApiResponse<>(null, "A required reference was missing or null. Please check your input and try again.", false, Instant.now()));  
	}
	
	@ExceptionHandler(EntityNotFoundException.class) 
	public ResponseEntity<ApiResponse<String>> notFound(EntityNotFoundException ex){ 
		return ResponseEntity.status(404).body(new ApiResponse<>(null, ex.getMessage() != null ? ex.getMessage() : "Requested resource not found.", false, Instant.now()));   
	}
	
	@ExceptionHandler(IllegalArgumentException.class)  
	public ResponseEntity<ApiResponse<String>> badRequest(IllegalArgumentException ex){ 
		return ResponseEntity.status(400).body(new ApiResponse<>(null, ex.getMessage(), false, Instant.now())); 
	}
	
	@ExceptionHandler(TransientPropertyValueException.class)  
	public ResponseEntity<ApiResponse<String>> foreign(TransientPropertyValueException ex){  
		return ResponseEntity.status(400).body(new ApiResponse<>(null, ex.getMessage(), false, Instant.now())); 
	}
	
	@ExceptionHandler(DataIntegrityViolationException.class)
	public ResponseEntity<ApiResponse<String>> dataIntegrityViolation(DataIntegrityViolationException ex) {  
		log.error("Data integrity violation: ", ex);
		return ResponseEntity.status(400).body(new ApiResponse<>(null,
				"The requested change conflicts with existing data.", false, Instant.now())); 
	}

	@ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
	public ResponseEntity<ApiResponse<String>> handleAccessDenied(org.springframework.security.access.AccessDeniedException ex) {
		return ResponseEntity.status(403).body(new ApiResponse<>(null, "Access Denied: You do not have permissions for this action.", false, Instant.now()));
	}

	@ExceptionHandler(org.springframework.web.server.ResponseStatusException.class)
	public ResponseEntity<ApiResponse<String>> handleResponseStatus(
			org.springframework.web.server.ResponseStatusException ex) {
		String message = ex.getReason() != null ? ex.getReason() : "Request could not be completed.";
		return ResponseEntity.status(ex.getStatusCode())
				.body(new ApiResponse<>(null, message, false, Instant.now()));
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiResponse<String>> handleGenericException(Exception ex) {
		log.error("Unhandled server exception: ", ex);
		return ResponseEntity.status(500).body(new ApiResponse<>(null,
				"An unexpected server error occurred.", false, Instant.now()));
	}
}
