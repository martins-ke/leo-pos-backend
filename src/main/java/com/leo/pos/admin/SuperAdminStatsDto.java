package com.leo.pos.admin;

import java.math.BigDecimal;

public record SuperAdminStatsDto(
		long totalBusinesses,
		long activeBusinesses,
		long totalOutlets,
		long totalCashiers,
		long totalSalesTransactions,
		BigDecimal totalGrossProcessedVolume
) {}
