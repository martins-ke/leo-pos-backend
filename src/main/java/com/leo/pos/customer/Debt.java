package com.leo.pos.customer;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import org.hibernate.annotations.CreationTimestamp;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "customer_debts")
public class Debt {

	@Id
	private String id;

	@ManyToOne
	@JoinColumn(name = "customer_id")
	@JsonIgnore
	private Customer customer;

	@Column(name = "sale_id", unique = true)
	private String saleId;

	@Column(nullable = false, precision = 12, scale = 2)
	private BigDecimal debtAmount;

	@Column(nullable = false, precision = 12, scale = 2)
	private BigDecimal outstandingBalance;

	@Column(nullable = false)
	private String status; 

	private String description;
	private LocalDate dueDate;
	@Column(nullable = false, updatable = false) 
	private String createdBy;

	@CreationTimestamp
	@Column(updatable = false)
	private Instant createdAt;

	private Instant paidAt;
	private Instant purgeAt;

	protected Debt() {}

	public Debt(String id, Customer customer, BigDecimal amount, String description,
			LocalDate dueDate, String createdBy) {
		this(id, customer, amount, description, dueDate, createdBy, null);
	}

	public Debt(String id, Customer customer, BigDecimal amount, String description,
			LocalDate dueDate, String createdBy, String saleId) {
		this.id = id;
		this.customer = customer;
		this.saleId = saleId;
		this.debtAmount = amount;
		this.outstandingBalance = amount;
		this.description = description;
		this.dueDate = dueDate;
		this.createdBy = createdBy;
		this.status = "OPEN";
	}

	public void applyPayment(BigDecimal amount, Instant paidTime) {
		outstandingBalance = outstandingBalance.subtract(amount);
		if (outstandingBalance.signum() == 0) {
			status = "SETTLED";
			paidAt = paidTime;
			purgeAt = paidTime.plusSeconds(7L * 24 * 60 * 60);
		}
	}

	public String getId() { return id; }
	public Customer getCustomer() { return customer; }
	public String getSaleId() { return saleId; }
	public BigDecimal getPrincipal() { return debtAmount; }
	public BigDecimal getOutstandingBalance() { return outstandingBalance; }
	public String getStatus() { return status; }
	public String getDescription() { return description; }
	public LocalDate getDueDate() { return dueDate; }
	public String getCreatedBy() { return createdBy; }
	public Instant getCreatedAt() { return createdAt; }
	public Instant getPaidAt() { return paidAt; }
	public Instant getPurgeAt() { return purgeAt; }
}
