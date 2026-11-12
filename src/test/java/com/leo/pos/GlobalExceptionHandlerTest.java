package com.leo.pos;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.sql.SQLException;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class GlobalExceptionHandlerTest {

	private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

	@Test
	void databaseErrorsDoNotExposeSqlOrDriverDetails() {
		ResponseEntity<ApiResponse<String>> sqlResponse =
				handler.handleSqlError(new SQLException("private-table-secret"));
		ResponseEntity<ApiResponse<String>> dataAccessResponse =
				handler.handleDataAccessException(
						new DataAccessResourceFailureException("private-table-secret"));
		ResponseEntity<ApiResponse<String>> integrityResponse =
				handler.dataIntegrityViolation(
						new DataIntegrityViolationException("private-table-secret"));

		assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, sqlResponse.getStatusCode());
		assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, dataAccessResponse.getStatusCode());
		assertEquals(HttpStatus.BAD_REQUEST, integrityResponse.getStatusCode());
		assertFalse(sqlResponse.getBody().message().contains("private-table-secret"));
		assertFalse(dataAccessResponse.getBody().message().contains("private-table-secret"));
		assertFalse(integrityResponse.getBody().message().contains("private-table-secret"));
	}

	@Test
	void genericErrorsDoNotExposeInternalExceptionMessages() {
		ResponseEntity<ApiResponse<String>> response =
				handler.handleGenericException(new IllegalStateException("private implementation detail"));

		assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
		assertFalse(response.getBody().message().contains("private implementation detail"));
	}
}
