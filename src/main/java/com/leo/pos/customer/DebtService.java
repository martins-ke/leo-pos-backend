package com.leo.pos.customer;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.leo.pos.audit.AuditService;
import com.leo.pos.finance.AccountLedgerService;
import com.leo.pos.finance.IdempotencyService;
import com.leo.pos.security.ShopAccessService;
import com.leo.pos.sale.Sale;
import com.leo.pos.sale.SaleRepository;
import com.leo.pos.shop.Shop;
import com.leo.pos.staff.User;

import jakarta.persistence.EntityNotFoundException;

@Service
public class DebtService {

	private final CustomerRepository customerRepository;
	private final DebtRepository debtRepository;
	private final DebtPaymentRepository paymentRepository;
	private final ShopAccessService shopAccessService;
	private final AccountLedgerService accountLedger;
	private final AuditService auditService;
	private final IdempotencyService idempotencyService;
	private final SaleRepository saleRepository;

	public DebtService(CustomerRepository customerRepository, DebtRepository debtRepository,
			DebtPaymentRepository paymentRepository, ShopAccessService shopAccessService,
			AccountLedgerService accountLedger, AuditService auditService,
			IdempotencyService idempotencyService, SaleRepository saleRepository) {
		this.customerRepository = customerRepository;
		this.debtRepository = debtRepository;
		this.paymentRepository = paymentRepository;
		this.shopAccessService = shopAccessService;
		this.accountLedger = accountLedger;
		this.auditService = auditService;
		this.idempotencyService = idempotencyService;
		this.saleRepository = saleRepository;
	}

	@Transactional(readOnly = true)
	public Page<DebtView> list(String shopId, String status, int page, int size, User actor) {
		shopAccessService.requireShopAccess(shopId, actor);
		PageRequest pageRequest = PageRequest.of(page, Math.min(size, 100));
		Page<Debt> debts = "ALL".equalsIgnoreCase(status)
				? debtRepository.findByCustomerShopIdOrderByCreatedAtDesc(shopId, pageRequest)
				: debtRepository.findByCustomerShopIdAndStatusOrderByCreatedAtDesc(shopId,
						status == null || status.isBlank() ? "OPEN" : status.toUpperCase(), pageRequest);
		return debts.map(this::toView);
	}

	@Transactional
	public DebtView create(DebtRequest request, String idempotencyKey, User actor) {
		idempotencyService.reserve(idempotencyKey, "CUSTOMER_DEBT", request, actor);
		
		Shop shop = shopAccessService.requireShopAccess(request.shopId(), actor);
		Customer customer = customerRepository.findById(request.customerId())
				.orElseThrow(() -> new EntityNotFoundException("Customer not found ⚠"));
		if (!shop.getId().equals(customer.getShop().getId())) {
			throw new EntityNotFoundException("Customer not found in this shop ⚠");
		}
		if (!customer.isCreditEligible()) {
			throw new IllegalArgumentException("⚠ This customer is not approved for credit!");
		}
		if (request.dueDate() != null && request.dueDate().isBefore(java.time.LocalDate.now())) {
			throw new IllegalArgumentException("⚠ Due date cannot be in the past!");
		}
		Debt debt = debtRepository.save(new Debt(
				UUID.randomUUID().toString(), customer, request.amount(),
				request.description() == null ? null : request.description().trim(),
				request.dueDate(), actor.getId()+"-"+actor.getUsername()));
		
		auditService.record(shop.getId(), actor, "DEBT_CREATED", "DEBT", debt.getId(),
				"principal=" + request.amount());
		idempotencyService.complete(idempotencyKey, debt.getId());
		return toView(debt);
	}

	@Transactional
	public DebtView convertSaleToDebt(String saleId, String idempotencyKey, User actor) {
		idempotencyService.reserve(idempotencyKey, "SALE_TO_DEBT", saleId, actor);
		Sale sale = saleRepository.findById(saleId)
				.orElseThrow(() -> new EntityNotFoundException("Sale not found"));
		Shop shop = shopAccessService.requireShopAccess(sale.getShop().getId(), actor);
		Customer customer = sale.getCustomer();
		if (customer == null) {
			throw new IllegalArgumentException("A customer must be linked to the sale before it can become a debt");
		}
		if (!customer.isCreditEligible()) {
			throw new IllegalArgumentException("This customer is not approved for credit");
		}
		if ("DEBT".equalsIgnoreCase(sale.getPaymentMethod()) || debtRepository.existsBySaleId(saleId)) {
			throw new IllegalArgumentException("This sale is already recorded as a debt");
		}

		Debt debt = debtRepository.save(new Debt(
				UUID.randomUUID().toString(), customer, sale.getGrandTotal(),
				"Converted from sale receipt " + sale.getReceiptNumber(), null,
				actor.getId() + "-" + actor.getUsername(), sale.getId()));
		accountLedger.post(shop, "SALE_CREDIT_CONVERSION", "OUT", sale.getGrandTotal(),
				sale.getPaymentMethod(), "Sale converted to customer debt: " + sale.getReceiptNumber(),
				"SALE", sale.getId(), actor);
		sale.setPaymentMethod("DEBT");
		sale.setAmountTendered(BigDecimal.ZERO);
		sale.setChangeGiven(BigDecimal.ZERO);
		saleRepository.save(sale);
		auditService.record(shop.getId(), actor, "SALE_CONVERTED_TO_DEBT", "SALE", sale.getId(),
				"debtId=" + debt.getId() + ", amount=" + sale.getGrandTotal());
		idempotencyService.complete(idempotencyKey, debt.getId());
		return toView(debt);
	}

	@Transactional
	public void createForSale(Sale sale, Customer customer, User actor) {
		if (!customer.isCreditEligible()) {
			throw new IllegalArgumentException("This customer is not approved for credit");
		}
		Debt debt = debtRepository.save(new Debt(
				UUID.randomUUID().toString(), customer, sale.getGrandTotal(),
				"POS sale receipt " + sale.getReceiptNumber(), null,
				actor.getId() + "-" + actor.getUsername(), sale.getId()));
		auditService.record(sale.getShop().getId(), actor, "DEBT_CREATED_FROM_SALE", "DEBT", debt.getId(),
				"saleId=" + sale.getId() + ", amount=" + sale.getGrandTotal());
	}

	@Transactional
	public DebtView recordPayment(String debtId, DebtPaymentRequest request, String idempotencyKey, User actor) {
		idempotencyService.reserve(idempotencyKey, "DEBT_PAYMENT", debtId + "|" + request, actor);
		Debt debt = debtRepository.findById(debtId)
				.orElseThrow(() -> new EntityNotFoundException("Debt not found"));
		Customer customer = debt.getCustomer();
		if (customer == null) {
			throw new EntityNotFoundException("Debt customer not found");
		}
		Shop shop = shopAccessService.requireShopAccess(customer.getShop().getId(), actor);
		if (!"OPEN".equals(debt.getStatus())) {
			throw new IllegalArgumentException("This debt is already settled");
		}
		if (request.amount().compareTo(debt.getOutstandingBalance()) > 0) {
			throw new IllegalArgumentException("Payment exceeds the outstanding balance");
		}
		String paymentId = UUID.randomUUID().toString();
		DebtPayment payment = paymentRepository.save(new DebtPayment(
				paymentId, debt, request.amount(), actor.getId(), actor.getUsername()));
		debt.applyPayment(request.amount(), Instant.now());
		String method = request.paymentMethod() == null ? "CASH" : request.paymentMethod().toUpperCase();
		accountLedger.post(shop, "DEBT_REPAYMENT", "IN", request.amount(), method,
				"Customer debt repayment", null, null, actor);
		auditService.record(shop.getId(), actor, "DEBT_PAYMENT_RECORDED",
				"DEBT_PAYMENT", payment.getId(), "amount=" + request.amount());
		if ("SETTLED".equals(debt.getStatus())) {
			auditService.record(shop.getId(), actor, "DEBT_SETTLED", "DEBT", debt.getId(),
					"purgeAt=" + debt.getPurgeAt());
		}
		idempotencyService.complete(idempotencyKey, payment.getId());
		return toView(debt);
	}

	@Transactional(readOnly = true)
	public List<DebtPayment> paymentHistory(String debtId, User actor) {
		Debt debt = debtRepository.findById(debtId)
				.orElseThrow(() -> new EntityNotFoundException("Debt not found"));
		Customer customer = debt.getCustomer();
		if (customer == null) {
			throw new EntityNotFoundException("Debt customer not found");
		}
		shopAccessService.requireShopAccess(customer.getShop().getId(), actor);
		return paymentRepository.findByDebtIdOrderByReceivedAtAsc(debtId);
	}

	private DebtView toView(Debt debt) {
		Customer customer = debt.getCustomer();
		return new DebtView(
				debt.getId(), customer != null ? customer.getId() : null,
				customer != null ? customer.getName() : "Deleted customer",
				customer != null ? customer.getShop().getId() : null,
				debt.getPrincipal(), debt.getOutstandingBalance(),
				debt.getDescription(), debt.getDueDate(), debt.getStatus(),
				debt.getCreatedBy(), debt.getCreatedAt(), 
				debt.getPaidAt(), debt.getPurgeAt(), debt.getSaleId());
	} 
}
