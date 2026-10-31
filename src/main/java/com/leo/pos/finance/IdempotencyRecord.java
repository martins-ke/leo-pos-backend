package com.leo.pos.finance;

import java.time.Instant;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "idempotency_records")
public class IdempotencyRecord {

	@Id
	@Column(length = 100)
	private String id;
	@Column(nullable = false)
	private String actorId;
	@Column(nullable = false)
	private String operation;
	@Column(nullable = false, length = 64)
	private String fingerprint;
	private String resultId;
	@CreationTimestamp
	private Instant createdAt;

	protected IdempotencyRecord() {}

	public IdempotencyRecord(String id, String actorId, String operation, String fingerprint) {
		this.id = id;
		this.actorId = actorId;
		this.operation = operation;
		this.fingerprint = fingerprint;
	}

	public String getId() { return id; }
	public String getActorId() { return actorId; }
	public String getOperation() { return operation; }
	public String getFingerprint() { return fingerprint; }
	public String getResultId() { return resultId; }
	public void setResultId(String resultId) { this.resultId = resultId; }
}
