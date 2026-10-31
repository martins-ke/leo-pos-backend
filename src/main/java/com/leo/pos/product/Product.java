package com.leo.pos.product;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.leo.pos.shop.Shop;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "products")
public class Product {

	@Id private String id;
	
	private String name;
	@JsonProperty("buying_price") 
	private BigDecimal buyingPrice;
	@JsonProperty("selling_price") 
	private BigDecimal sellingPrice;
	private float quantity;
	@JsonProperty("min_stock_level") 
	private float minStockLevel;
	private String unit;
	private boolean active;
	@JoinColumn(name = "shop_id") @ManyToOne
	private Shop shop;
	
	public Product() {}

	public Product(String id, String name, BigDecimal buyingPrice, BigDecimal sellingPrice,
			float quantity, float minStockLevel, String unit, boolean active, Shop shop) {
		super();
		this.id = id;
		this.name = name;
		this.buyingPrice = buyingPrice;
		this.sellingPrice = sellingPrice;
		this.quantity = quantity;
		this.minStockLevel = minStockLevel;
		this.unit = unit;
		this.active = active;
		this.shop = shop;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public BigDecimal getBuyingPrice() {
		return buyingPrice;
	}

	public void setBuyingPrice(BigDecimal buyingPrice) {
		this.buyingPrice = buyingPrice;
	}

	public BigDecimal getSellingPrice() {
		return sellingPrice;
	}

	public void setSellingPrice(BigDecimal sellingPrice) {
		this.sellingPrice = sellingPrice;
	}

	public float getQuantity() {
		return quantity;
	}

	public void setQuantity(float quantity) {
		this.quantity = quantity;
	}

	public float getMinStockLevel() {
		return minStockLevel;
	}

	public void setMinStockLevel(float minStockLevel) {
		this.minStockLevel = minStockLevel;
	}

	public String getUnit() {
		return unit;
	}

	public void setUnit(String unit) {
		this.unit = unit;
	}

	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}

	public Shop getShop() {
		return shop;
	}

	public void setShop(Shop shop) {
		this.shop = shop;
	}
}
