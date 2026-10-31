package com.leo.pos.customer;

import java.time.Instant;

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
@Table(name = "customers")
public class Customer {

	@Id
	private String id;

	@Column(nullable = false)
	private String name;

	private String phone;
	private String email;
	private boolean creditEligible;

	@ManyToOne
	@JoinColumn(name = "shop_id", nullable = false)
	@JsonIgnore
	private Shop shop;

	@Column(nullable = false, updatable = false)
	private String createdBy;

	@CreationTimestamp
	@Column(updatable = false)
	private Instant createdAt;

	protected Customer() {}

	public Customer(String id, String name, String phone, String email, Shop shop, String createdBy) {
		this.id = id;
		this.name = name;
		this.phone = phone;
		this.email = email;
		this.shop = shop;
		this.createdBy = createdBy;
		this.creditEligible = false;
	}

	public String getId() { return id; }
	public String getName() { return name; }
	public void setName(String name) { this.name = name; }
	public String getPhone() { return phone; }
	public void setPhone(String phone) { this.phone = phone; }
	public String getEmail() { return email; }
	public void setEmail(String email) { this.email = email; }
	public boolean isCreditEligible() { return creditEligible; }
	public void setCreditEligible(boolean creditEligible) { this.creditEligible = creditEligible; }
	public Shop getShop() { return shop; }
	public String getCreatedBy() { return createdBy; }
	public Instant getCreatedAt() { return createdAt; }
}
