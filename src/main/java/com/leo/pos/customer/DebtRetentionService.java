package com.leo.pos.customer;

import java.time.Instant;
import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.leo.pos.audit.AuditEventRepository;

@Service
public class DebtRetentionService {

	private final DebtRepository debtRepository;
	private final DebtPaymentRepository paymentRepository;
	private final AuditEventRepository auditEventRepository;

	public DebtRetentionService(DebtRepository debtRepository,
			DebtPaymentRepository paymentRepository, AuditEventRepository auditEventRepository) {
		this.debtRepository = debtRepository;
		this.paymentRepository = paymentRepository;
		this.auditEventRepository = auditEventRepository;
	}

	@Scheduled(fixedDelayString = "${finance.debt-purge-interval-ms:3600000}")
	@Transactional
	public void purgeSettledDebtRecords() {
		List<Debt> expired = debtRepository.findByStatusAndPurgeAtLessThanEqual(
				"SETTLED", Instant.now(), PageRequest.of(0, 100));
		for (Debt debt : expired) {
			List<String> paymentIds = paymentRepository.findByDebtIdOrderByReceivedAtAsc(debt.getId())
					.stream().map(DebtPayment::getId).toList();
			paymentRepository.deleteByDebtId(debt.getId());
			auditEventRepository.deleteByResourceTypeAndResourceIdIn("DEBT", List.of(debt.getId()));
			if (!paymentIds.isEmpty()) {
				auditEventRepository.deleteByResourceTypeAndResourceIdIn("DEBT_PAYMENT", paymentIds);
			}
			debtRepository.delete(debt);
		}
	}
}
