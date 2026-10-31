package com.leo.pos.finance;

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
@Table(name = "account_transactions")
public class AccountTransaction {

	@Id
	private String id;

	@ManyToOne
	@JoinColumn(name = "account_id", nullable = false)
	@JsonIgnore
	private ShopAccount account;

	@Column(nullable = false)
	private String type;
	@Column(nullable = false)
	private String direction;
	@Column(nullable = false, precision = 12, scale = 2)
	private BigDecimal amount;
	private String paymentMethod;
	private String description;
	private String referenceType;
	private String referenceId;
	@Column(nullable = false, updatable = false)
	private String actorId;
	@Column(nullable = false, updatable = false)
	private String actorName;
	@CreationTimestamp
	@Column(updatable = false)
	private Instant occurredAt;

	protected AccountTransaction() {}

	public AccountTransaction(String id, ShopAccount account, String type, String direction, BigDecimal amount,
			String paymentMethod, String description, String referenceType, String referenceId,
			String actorId, String actorName) {
		this.id = id;
		this.account = account;
		this.type = type;
		this.direction = direction;
		this.amount = amount;
		this.paymentMethod = paymentMethod;
		this.description = description;
		this.referenceType = referenceType;
		this.referenceId = referenceId;
		this.actorId = actorId;
		this.actorName = actorName;
	}

	public String getId() { return id; }
	public ShopAccount getAccount() { return account; }
	public String getType() { return type; }
	public String getDirection() { return direction; }
	public BigDecimal getAmount() { return amount; }
	public String getPaymentMethod() { return paymentMethod; }
	public String getDescription() { return description; }
	public String getReferenceType() { return referenceType; }
	public String getReferenceId() { return referenceId; }
	public String getActorId() { return actorId; }
	public String getActorName() { return actorName; }
	public Instant getOccurredAt() { return occurredAt; }
}
