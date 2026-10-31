package com.leo.pos.finance;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

import org.springframework.http.HttpStatus;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.leo.pos.staff.User;

@Service
public class IdempotencyService {

	private final IdempotencyRecordRepository repository;

	public IdempotencyService(IdempotencyRecordRepository repository) {
		this.repository = repository;
	}

	@Transactional
	public void reserve(String key, String operation, Object request, User actor) {
		if (key == null || key.isBlank() || key.length() > 100) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A valid Idempotency-Key header is required");
		}
		String fingerprint = fingerprint(operation + "|" + request);
		if (repository.existsById(key)) {
			throw new ResponseStatusException(HttpStatus.CONFLICT,
					"This request key was already used. Refresh the record before retrying.");
		}
		try {
			repository.saveAndFlush(new IdempotencyRecord(key, actor.getId(), operation, fingerprint));
		} catch (DataIntegrityViolationException exception) {
			throw new ResponseStatusException(HttpStatus.CONFLICT,
					"This request key was already used. Refresh the record before retrying.");
		}
	}

	@Transactional
	public void complete(String key, String resultId) {
		IdempotencyRecord record = repository.findById(key)
				.orElseThrow(() -> new IllegalStateException("Idempotency reservation was not found"));
		record.setResultId(resultId);
	}

	private String fingerprint(String value) {
		try {
			byte[] hash = MessageDigest.getInstance("SHA-256")
					.digest(value.getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(hash);
		} catch (NoSuchAlgorithmException exception) {
			throw new IllegalStateException("SHA-256 is unavailable", exception);
		}
	}
}
