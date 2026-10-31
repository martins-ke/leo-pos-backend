package com.leo.pos.finance;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record CapitalRequest(
		@NotNull @DecimalMin(value = "0.01") @Digits(integer = 10, fraction = 2) BigDecimal amount,
		@Pattern(regexp = "(?i)CASH|MPESA|CARD|BANK_TRANSFER", message = "Unsupported payment method")
		String paymentMethod,
		String description
) {}
