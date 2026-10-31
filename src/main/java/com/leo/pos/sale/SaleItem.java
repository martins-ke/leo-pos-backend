package com.leo.pos.sale;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.leo.pos.product.Product;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "sale_items")
public class SaleItem {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne
	@JoinColumn(name = "product_id")
	private Product product;

	@Column(name = "product_name")
	private String productName;

	private float quantity;

	@Column(name = "unit_price", precision = 12, scale = 2)
	private BigDecimal unitPrice;

	@Column(precision = 12, scale = 2)
	private BigDecimal subtotal;

	@Column(name = "unit_cost", precision = 12, scale = 2)
	private BigDecimal unitCost;

	@ManyToOne
	@JoinColumn(name = "sale_id")
	@JsonBackReference
	private Sale sale;

	public SaleItem() {
	}

	public SaleItem(Product product, String productName, float quantity, BigDecimal unitPrice, BigDecimal subtotal) {
		this(product, productName, quantity, unitPrice, subtotal,
				product != null ? product.getBuyingPrice() : null);
	}

	public SaleItem(Product product, String productName, float quantity, BigDecimal unitPrice, BigDecimal subtotal,
			BigDecimal unitCost) {
		this.product = product;
		this.productName = productName;
		this.quantity = quantity;
		this.unitPrice = unitPrice;
		this.subtotal = subtotal;
		this.unitCost = unitCost;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Product getProduct() {
		return product;
	}

	public void setProduct(Product product) {
		this.product = product;
	}

	public String getProductName() {
		return productName;
	}

	public void setProductName(String productName) {
		this.productName = productName;
	}

	public float getQuantity() {
		return quantity;
	}

	public void setQuantity(float quantity) {
		this.quantity = quantity;
	}

	public BigDecimal getUnitPrice() {
		return unitPrice;
	}

	public void setUnitPrice(BigDecimal unitPrice) {
		this.unitPrice = unitPrice;
	}

	public BigDecimal getSubtotal() {
		return subtotal;
	}

	public void setSubtotal(BigDecimal subtotal) {
		this.subtotal = subtotal;
	}

	public BigDecimal getUnitCost() {
		return unitCost;
	}

	public void setUnitCost(BigDecimal unitCost) {
		this.unitCost = unitCost;
	}

	public Sale getSale() {
		return sale;
	}

	public void setSale(Sale sale) {
		this.sale = sale;
	}
}
