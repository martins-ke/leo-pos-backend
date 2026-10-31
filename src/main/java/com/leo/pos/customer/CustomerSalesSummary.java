package com.leo.pos.customer;

import java.math.BigDecimal;

public interface CustomerSalesSummary {
	String getCustomerId();
	long getSalesCount();
	BigDecimal getSalesTotal();
}
