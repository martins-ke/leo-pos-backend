package com.leo.pos.sale;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record SaleItemRequest(
		@NotNull(message = "Product ID is required!")
		String productId,
		@NotNull(message = "Quantity is required!")
		@Positive(message = "Quantity must be greater than zero!")
		Float quantity,
		@NotNull(message = "Unit price is required!")
		BigDecimal unitPrice
) {}
