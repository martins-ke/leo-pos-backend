package com.leo.pos.customer;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface DebtPaymentRepository extends JpaRepository<DebtPayment, String> {
	List<DebtPayment> findByDebtIdOrderByReceivedAtAsc(String debtId);
	void deleteByDebtId(String debtId);
}
