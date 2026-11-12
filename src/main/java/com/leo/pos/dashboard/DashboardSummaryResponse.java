package com.leo.pos.dashboard;

import java.math.BigDecimal;
import java.util.List;

import com.leo.pos.product.Product;

public record DashboardSummaryResponse(
		BigDecimal todayRevenue,
		BigDecimal todayGrossProfit,
		BigDecimal monthRevenue,
		BigDecimal monthGrossProfit,
		BigDecimal todayCashCollected,
		BigDecimal todayMpesaCollected,
		BigDecimal todayCardCollected,
		BigDecimal todayBankTransferCollected,
		BigDecimal todayNetProfit,
		BigDecimal monthNetProfit,
		long totalSalesToday,
		long totalProducts,
		long lowStockCount,
		List<RecentSaleDto> recentSales,
		List<Product> lowStockProducts
) {}
