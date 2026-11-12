package com.leo.pos.customer;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record DebtView(
		String id,
		String customerId,
		String customerName,
		String shopId,
		BigDecimal principal,
		BigDecimal outstandingBalance,
		String description,
		LocalDate dueDate,
		String status,
		String createdBy, 
		Instant createdAt,
		Instant paidAt,
		Instant purgeAt,
		String saleId
) {}
