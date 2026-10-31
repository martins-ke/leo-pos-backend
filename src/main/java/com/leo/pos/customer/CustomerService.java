package com.leo.pos.customer;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.leo.pos.audit.AuditService;
import com.leo.pos.finance.IdempotencyService;
import com.leo.pos.sale.SaleRepository;
import com.leo.pos.security.ShopAccessService;
import com.leo.pos.shop.Shop;
import com.leo.pos.staff.User;

import jakarta.persistence.EntityNotFoundException;

@Service
public class CustomerService {

	private final CustomerRepository customerRepository;
	private final DebtRepository debtRepository;
	private final SaleRepository saleRepository;
	private final ShopAccessService shopAccessService;
	private final AuditService auditService;
	private final IdempotencyService idempotencyService;

	public CustomerService(CustomerRepository customerRepository, DebtRepository debtRepository,
			SaleRepository saleRepository, ShopAccessService shopAccessService,
			AuditService auditService, IdempotencyService idempotencyService) {
		this.customerRepository = customerRepository;
		this.debtRepository = debtRepository;
		this.saleRepository = saleRepository;
		this.shopAccessService = shopAccessService;
		this.auditService = auditService;
		this.idempotencyService = idempotencyService;
	}

	@Transactional(readOnly = true)
	public Page<CustomerView> list(String shopId, int page, int size, User actor) {
		shopAccessService.requireShopAccess(shopId, actor);
		Page<Customer> customers = customerRepository.findByShopIdOrderByNameAsc(
				shopId, PageRequest.of(page, Math.min(size, 100)));
		List<String> ids = customers.getContent().stream().map(Customer::getId).toList();
		Map<String, CustomerSalesSummary> summaries = new HashMap<>();
		if (!ids.isEmpty()) {
			saleRepository.summarizeSalesByCustomerIdIn(ids)
					.forEach(summary -> summaries.put(summary.getCustomerId(), summary));
		}
		return customers.map(customer -> toView(customer, summaries.get(customer.getId())));
	}

	@Transactional
	public CustomerView create(CustomerRequest request, String idempotencyKey, User actor) {
		idempotencyService.reserve(idempotencyKey, "CUSTOMER", request, actor);
		Shop shop = shopAccessService.requireShopAccess(request.shopId(), actor);
		Customer customer = customerRepository.save(new Customer(
				UUID.randomUUID().toString(), request.name().trim(),
				normalize(request.phone()), normalize(request.email()), shop, actor.getId()));
		auditService.record(shop.getId(), actor, "CUSTOMER_CREATED", "CUSTOMER", customer.getId(), null);
		idempotencyService.complete(idempotencyKey, customer.getId());
		return toView(customer, null);
	}

	@Transactional
	public CustomerView setEligibility(String customerId, boolean eligible, User actor) {
		Customer customer = customerRepository.findById(customerId)
				.orElseThrow(() -> new EntityNotFoundException("Customer not found"));
		Shop shop = shopAccessService.requireOwnerShop(customer.getShop().getId(), actor);
		customer.setCreditEligible(eligible);
		auditService.record(shop.getId(), actor, "CUSTOMER_CREDIT_ELIGIBILITY_CHANGED",
				"CUSTOMER", customerId, "creditEligible=" + eligible);
		return toView(customer, null);
	}

	@Transactional
	public void delete(String customerId, User actor) {
		Customer customer = customerRepository.findById(customerId)
				.orElseThrow(() -> new EntityNotFoundException("Customer not found"));
		Shop shop = shopAccessService.requireOwnerShop(customer.getShop().getId(), actor);
		if (debtRepository.existsByCustomerId(customerId)) {
			throw new IllegalArgumentException("Settle and wait until the customer's debt records are purged before deleting the customer");
		}
		saleRepository.anonymizeCustomerSales(customerId);
		auditService.record(shop.getId(), actor, "CUSTOMER_DELETED", "CUSTOMER", customerId, "Historical sales anonymized");
		customerRepository.delete(customer);
	}

	private CustomerView toView(Customer customer, CustomerSalesSummary summary) {
		return new CustomerView(
				customer.getId(), customer.getShop().getId(), customer.getName(),
				customer.getPhone(), customer.getEmail(), customer.isCreditEligible(),
				summary != null ? summary.getSalesCount() : 0,
				summary != null && summary.getSalesTotal() != null ? summary.getSalesTotal() : BigDecimal.ZERO,
				customer.getCreatedAt());
	}

	private String normalize(String value) {
		return value == null || value.isBlank() ? null : value.trim();
	}
}
