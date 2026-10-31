package com.leo.pos.customer;

import java.math.BigDecimal;
import java.time.Instant;

public record CustomerView(
		String id,
		String shopId,
		String name,
		String phone,
		String email,
		boolean creditEligible,
		long previousSalesCount,
		BigDecimal previousSalesTotal,
		Instant createdAt
) {}
