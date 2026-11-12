package com.leo.pos.product;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ProductRequest(
		@NotBlank(message = "Product name required!")
		String name,
		@NotNull(message = "Buying price required for profit calculations!")
		BigDecimal buyingPrice,
		@NotNull(message = "Selling price required for profit calculations!")
		BigDecimal sellingPrice,
		@NotNull(message = "Initial quantity required!")
		Float quantityInStock,
		@NotNull(message = "Minimum stock level required for alerts!")
		Float minStockLevel,
		@NotBlank(message = "Product units required!")
		String unit,
		String categoryId,
		boolean clearCategory
) {
	public ProductRequest(String name, BigDecimal buyingPrice, BigDecimal sellingPrice,
			Float quantityInStock, Float minStockLevel, String unit) {
		this(name, buyingPrice, sellingPrice, quantityInStock, minStockLevel, unit, null, false);
	}

	public ProductRequest(String name, BigDecimal buyingPrice, BigDecimal sellingPrice,
			Float quantityInStock, Float minStockLevel, String unit, String categoryId) {
		this(name, buyingPrice, sellingPrice, quantityInStock, minStockLevel, unit, categoryId, false);
	}
}
