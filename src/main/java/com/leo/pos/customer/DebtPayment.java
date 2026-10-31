package com.leo.pos.customer;

import java.math.BigDecimal;
import java.time.Instant;

import org.hibernate.annotations.CreationTimestamp;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "debt_payments")
public class DebtPayment {

	@Id
	private String id;

	@ManyToOne
	@JoinColumn(name = "debt_id", nullable = false)
	@JsonIgnore
	private Debt debt;

	@Column(nullable = false, precision = 12, scale = 2)
	private BigDecimal amount;

	@Column(nullable = false, updatable = false)
	private String receivedBy;
	@Column(nullable = false, updatable = false)
	private String receivedByName;

	@CreationTimestamp
	@Column(updatable = false)
	private Instant receivedAt;

	protected DebtPayment() {}

	public DebtPayment(String id, Debt debt, BigDecimal amount, String receivedBy, String receivedByName) {
		this.id = id;
		this.debt = debt;
		this.amount = amount;
		this.receivedBy = receivedBy;
		this.receivedByName = receivedByName;
	}

	public String getId() { return id; }
	public Debt getDebt() { return debt; }
	public BigDecimal getAmount() { return amount; }
	public String getReceivedBy() { return receivedBy; }
	public String getReceivedByName() { return receivedByName; }
	public Instant getReceivedAt() { return receivedAt; }
}
