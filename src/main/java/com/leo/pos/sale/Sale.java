package com.leo.pos.sale;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.leo.pos.shop.Shop;
import com.leo.pos.staff.User;
import com.leo.pos.customer.Customer;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Entity
@Table(name = "sales")
public class Sale {

	@Id
	private String id;

	@Column(name = "receipt_number", unique = true)
	private String receiptNumber;

	@Column(name = "grand_total", precision = 12, scale = 2)
	private BigDecimal grandTotal;

	@Column(name = "amount_tendered", precision = 12, scale = 2)
	private BigDecimal amountTendered;

	@Column(name = "change_given", precision = 12, scale = 2)
	private BigDecimal changeGiven;

	@Column(name = "payment_method")
	private String paymentMethod;

	@CreationTimestamp
	@Column(name = "created_at", updatable = false)
	private LocalDateTime createdAt;

	@ManyToOne
	@JoinColumn(name = "shop_id")
	private Shop shop;

	@ManyToOne
	@JoinColumn(name = "cashier_id")
	private User cashier;

	@ManyToOne
	@JoinColumn(name = "customer_id")
	@JsonIgnoreProperties({"phone", "email", "creditEligible", "createdBy", "createdAt", "shop"})
	private Customer customer;

	@OneToMany(mappedBy = "sale", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
	@JsonManagedReference
	private List<SaleItem> items = new ArrayList<>();

	public Sale() {
	}

	public Sale(String id, String receiptNumber, BigDecimal grandTotal, BigDecimal amountTendered,
			BigDecimal changeGiven, String paymentMethod, Shop shop, User cashier) {
		this.id = id;
		this.receiptNumber = receiptNumber;
		this.grandTotal = grandTotal;
		this.amountTendered = amountTendered;
		this.changeGiven = changeGiven;
		this.paymentMethod = paymentMethod != null ? paymentMethod : "CASH";
		this.shop = shop;
		this.cashier = cashier;
		this.createdAt = LocalDateTime.now();
	}

	public void addItem(SaleItem item) {
		items.add(item);
		item.setSale(this);
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getReceiptNumber() {
		return receiptNumber;
	}

	public void setReceiptNumber(String receiptNumber) {
		this.receiptNumber = receiptNumber;
	}

	public BigDecimal getGrandTotal() {
		return grandTotal;
	}

	public void setGrandTotal(BigDecimal grandTotal) {
		this.grandTotal = grandTotal;
	}

	public BigDecimal getAmountTendered() {
		return amountTendered;
	}

	public void setAmountTendered(BigDecimal amountTendered) {
		this.amountTendered = amountTendered;
	}

	public BigDecimal getChangeGiven() {
		return changeGiven;
	}

	public void setChangeGiven(BigDecimal changeGiven) {
		this.changeGiven = changeGiven;
	}

	public String getPaymentMethod() {
		return paymentMethod;
	}

	public void setPaymentMethod(String paymentMethod) {
		this.paymentMethod = paymentMethod;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(LocalDateTime createdAt) {
		this.createdAt = createdAt;
	}

	public Shop getShop() {
		return shop;
	}

	public void setShop(Shop shop) {
		this.shop = shop;
	}

	public User getCashier() {
		return cashier;
	}

	public void setCashier(User cashier) {
		this.cashier = cashier;
	}

	public Customer getCustomer() {
		return customer;
	}

	public void setCustomer(Customer customer) {
		this.customer = customer;
	}

	public List<SaleItem> getItems() {
		return items;
	}

	public void setItems(List<SaleItem> items) {
		this.items = items;
	}
}
