package com.leo.pos.finance;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record ExpenseRequest(
		@NotBlank String shopId,
		@NotBlank String category,
		@NotNull @DecimalMin(value = "0.01") @Digits(integer = 10, fraction = 2) BigDecimal amount,
		@NotNull LocalDate expenseDate,
		String payee,
		@NotBlank @Pattern(regexp = "(?i)CASH|MPESA|CARD|BANK_TRANSFER", message = "Unsupported payment method")
		String paymentMethod,
		String notes
) {}
