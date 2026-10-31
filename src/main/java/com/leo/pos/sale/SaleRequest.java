package com.leo.pos.sale;

import java.math.BigDecimal;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record SaleRequest(
		String shopId,
		String cashierId,
		@NotEmpty(message = "Sale items cannot be empty")
		@Valid
		List<SaleItemRequest> items,
		@NotNull(message = "Grand total is required")
		BigDecimal grandTotal,
		@NotNull(message = "Amount tendered is required")
		BigDecimal amountTendered,
		BigDecimal changeGiven,
		String paymentMethod,
		String customerId
) {}
