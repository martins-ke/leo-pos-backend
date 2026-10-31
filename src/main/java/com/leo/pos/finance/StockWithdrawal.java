package com.leo.pos.finance;

import java.math.BigDecimal;
import java.time.Instant;

import org.hibernate.annotations.CreationTimestamp;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.leo.pos.product.Product;
import com.leo.pos.shop.Shop;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "stock_withdrawals")
public class StockWithdrawal {

	@Id
	private String id;
	@ManyToOne
	@JoinColumn(name = "shop_id", nullable = false)
	@JsonIgnore
	private Shop shop;
	@ManyToOne
	@JoinColumn(name = "product_id", nullable = false)
	private Product product;
	@Column(nullable = false)
	private float quantity;
	@Column(nullable = false, precision = 12, scale = 2)
	private BigDecimal unitCost;
	@Column(nullable = false, precision = 12, scale = 2)
	private BigDecimal totalCost;
	@Column(nullable = false)
	private String reason;
	@Column(nullable = false, updatable = false)
	private String actorId;
	@Column(nullable = false, updatable = false)
	private String actorName;
	@CreationTimestamp
	@Column(updatable = false)
	private Instant occurredAt;

	protected StockWithdrawal() {}

	public StockWithdrawal(String id, Shop shop, Product product, float quantity, BigDecimal unitCost,
			BigDecimal totalCost, String reason, String actorId, String actorName) {
		this.id = id;
		this.shop = shop;
		this.product = product;
		this.quantity = quantity;
		this.unitCost = unitCost;
		this.totalCost = totalCost;
		this.reason = reason;
		this.actorId = actorId;
		this.actorName = actorName;
	}

	public String getId() { return id; }
	public Shop getShop() { return shop; }
	public Product getProduct() { return product; }
	public float getQuantity() { return quantity; }
	public BigDecimal getUnitCost() { return unitCost; }
	public BigDecimal getTotalCost() { return totalCost; }
	public String getReason() { return reason; }
	public String getActorId() { return actorId; }
	public String getActorName() { return actorName; }
	public Instant getOccurredAt() { return occurredAt; }
}
