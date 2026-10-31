package com.leo.pos.finance;

import java.math.BigDecimal;
import java.time.Instant;

public record AccountTransactionView(
		String id,
		String type,
		String direction,
		BigDecimal amount,
		String paymentMethod,
		String description,
		String actorName,
		Instant occurredAt
) {
	public static AccountTransactionView from(AccountTransaction transaction) {
		return new AccountTransactionView(
				transaction.getId(), transaction.getType(), transaction.getDirection(), transaction.getAmount(),
				transaction.getPaymentMethod(), transaction.getDescription(), transaction.getActorName(),
				transaction.getOccurredAt());
	}
}
