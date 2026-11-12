package com.leo.pos.admin;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record TenantSummaryDto(
		String ownerId,
		String username,
		String email,
		String status,
		int shopCount,
		int cashierCount,
		long totalTransactions,
		BigDecimal totalVolume,
		List<String> shopNames,
		LocalDate joinedDate
) {}
