package com.leo.pos.productCategories;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.leo.pos.shop.Shop;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "product_categories", uniqueConstraints = {
		@UniqueConstraint(name = "uk_product_category_shop_name", columnNames = { "shop_id", "name" })
})
public class ProductCategory {
	@Id
	private String id;

	@Column(nullable = false, length = 80)
	private String name;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "shop_id", nullable = false)
	@JsonIgnore
	private Shop shop;

	@Column(nullable = false, columnDefinition = "boolean default true")
	private boolean active = true;

	public ProductCategory() {}

	public ProductCategory(String id, String name, Shop shop) {
		super();
		this.id = id;
		this.name = name;
		this.shop = shop;
		this.active = true;
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

	@JsonProperty("shop_id")
	public String getShopId() {
		return shop == null ? null : shop.getId();
	}

	public Shop getShop() {
		return shop;
	}

	public void setShop(Shop shop) {
		this.shop = shop;
	}

	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}
}
