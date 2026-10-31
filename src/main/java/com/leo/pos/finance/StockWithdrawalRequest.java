package com.leo.pos.finance;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record StockWithdrawalRequest(
		@NotBlank String shopId,
		@NotBlank String productId,
		@NotNull @Positive Float quantity,
		@NotBlank String reason
) {}
