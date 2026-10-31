package com.leo.pos.finance;

import java.math.BigDecimal;

public record AccountSummary(
		String shopId,
		BigDecimal accountBalance,
		BigDecimal grossProfit,
		BigDecimal operatingExpenses,
		BigDecimal netProfit,
		BigDecimal ownerDrawings,
		BigDecimal remainingProfitAfterDrawings
) {}
