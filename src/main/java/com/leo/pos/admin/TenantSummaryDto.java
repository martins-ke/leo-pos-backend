package com.leo.pos.admin;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.leo.pos.user.Status;

public record TenantSummaryDto(
		String ownerId,
		String username,
		String email,
		Status status,
		int shopCount,
		int cashierCount,
		long totalTransactions,
		BigDecimal totalVolume,
		List<String> shopNames,
		LocalDate joinedDate
) {}
