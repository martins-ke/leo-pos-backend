package com.leo.pos.productCategories;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "product_categories")
public class ProductCategory {
	@Id private String id;
	private String name;
	@Column(name = "shop_id")
	@JsonProperty("shop_id")
	private String shopId;
	
	public ProductCategory() {}
	public ProductCategory(String id, String name, String shopId) { 
		super();
		this.id = id;
		this.name = name;
		this.shopId = shopId;
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
	public String getShopId() {
		return shopId;
	}
	public void setShopId(String shopId) {
		this.shopId = shopId;
	}
}
