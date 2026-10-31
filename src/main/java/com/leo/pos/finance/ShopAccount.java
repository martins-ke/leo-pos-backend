package com.leo.pos.finance;

import java.time.Instant;

import org.hibernate.annotations.CreationTimestamp;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.leo.pos.shop.Shop;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "shop_accounts")
public class ShopAccount {

	@Id
	private String id;

	@OneToOne
	@JoinColumn(name = "shop_id", nullable = false, unique = true)
	@JsonIgnore
	private Shop shop;

	@CreationTimestamp
	private Instant createdAt;

	protected ShopAccount() {}

	public ShopAccount(String id, Shop shop) {
		this.id = id;
		this.shop = shop;
	}

	public String getId() { return id; }
	public Shop getShop() { return shop; }
	public Instant getCreatedAt() { return createdAt; }
}
