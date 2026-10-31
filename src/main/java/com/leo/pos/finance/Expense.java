package com.leo.pos.finance;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import org.hibernate.annotations.CreationTimestamp;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.leo.pos.shop.Shop;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "expenses")
public class Expense {

	@Id
	private String id;
	@ManyToOne
	@JoinColumn(name = "shop_id", nullable = false)
	@JsonIgnore
	private Shop shop;
	@Column(nullable = false)
	private String category;
	@Column(nullable = false, precision = 12, scale = 2)
	private BigDecimal amount;
	@Column(nullable = false)
	private LocalDate expenseDate;
	private String payee;
	private String paymentMethod;
	private String notes;
	@Column(nullable = false)
	private boolean cashExpense;
	private String linkedResourceType;
	private String linkedResourceId;
	@Column(nullable = false, updatable = false)
	private String paidById;
	@Column(nullable = false, updatable = false)
	private String paidByName;
	@CreationTimestamp
	@Column(updatable = false)
	private Instant createdAt;

	protected Expense() {}

	public Expense(String id, Shop shop, String category, BigDecimal amount, LocalDate expenseDate,
			String payee, String paymentMethod, String notes, boolean cashExpense,
			String linkedResourceType, String linkedResourceId, String paidById, String paidByName) {
		this.id = id;
		this.shop = shop;
		this.category = category;
		this.amount = amount;
		this.expenseDate = expenseDate;
		this.payee = payee;
		this.paymentMethod = paymentMethod;
		this.notes = notes;
		this.cashExpense = cashExpense;
		this.linkedResourceType = linkedResourceType;
		this.linkedResourceId = linkedResourceId;
		this.paidById = paidById;
		this.paidByName = paidByName;
	}

	public String getId() { return id; }
	public Shop getShop() { return shop; }
	public String getCategory() { return category; }
	public BigDecimal getAmount() { return amount; }
	public LocalDate getExpenseDate() { return expenseDate; }
	public String getPayee() { return payee; }
	public String getPaymentMethod() { return paymentMethod; }
	public String getNotes() { return notes; }
	public boolean isCashExpense() { return cashExpense; }
	public String getLinkedResourceType() { return linkedResourceType; }
	public String getLinkedResourceId() { return linkedResourceId; }
	public String getPaidById() { return paidById; }
	public String getPaidByName() { return paidByName; }
	public Instant getCreatedAt() { return createdAt; }
}
