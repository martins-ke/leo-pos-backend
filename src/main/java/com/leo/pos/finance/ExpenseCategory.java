package com.leo.pos.finance;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.leo.pos.shop.Shop;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "expense_categories", uniqueConstraints = @UniqueConstraint(columnNames = {"shop_id", "name"}))
public class ExpenseCategory {

	@Id
	private String id;
	@ManyToOne
	@JoinColumn(name = "shop_id", nullable = false)
	@JsonIgnore
	private Shop shop;
	@Column(nullable = false)
	private String name;

	protected ExpenseCategory() {}

	public ExpenseCategory(String id, Shop shop, String name) {
		this.id = id;
		this.shop = shop;
		this.name = name;
	}

	public String getId() { return id; }
	public Shop getShop() { return shop; }
	public String getName() { return name; }
}
