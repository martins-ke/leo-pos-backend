package com.leo.pos.dashboard;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record RecentSaleDto(
		String id,
		String receiptNumber,
		String shopName,
		String cashierName,
		BigDecimal grandTotal,
		String paymentMethod,
		LocalDateTime createdAt
) {}
