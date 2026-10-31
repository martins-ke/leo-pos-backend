package com.leo.pos.finance;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record StockPurchaseRequest(
		@NotBlank String shopId,
		@NotBlank String productId,
		@NotNull @Positive Float quantity,
		@NotNull @DecimalMin(value = "0.01") @Digits(integer = 10, fraction = 2) BigDecimal unitCost,
		String paymentMethod,
		String notes
) {}
