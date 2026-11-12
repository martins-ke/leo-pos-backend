package com.leo.pos.sale;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.leo.pos.ApiResponse;
import com.leo.pos.audit.AuditService;
import com.leo.pos.customer.Customer;
import com.leo.pos.customer.CustomerRepository;
import com.leo.pos.customer.DebtService;
import com.leo.pos.finance.AccountLedgerService;
import com.leo.pos.finance.IdempotencyService;
import com.leo.pos.product.Product;
import com.leo.pos.product.ProductRepository;
import com.leo.pos.security.ShopAccessService;
import com.leo.pos.shop.Shop;
import com.leo.pos.shop.ShopRepository;
import com.leo.pos.staff.User;

import jakarta.persistence.EntityNotFoundException;

@Service
public class SaleService {

	private final SaleRepository saleRepo;
	private final ProductRepository productRepo;
	private final ShopRepository shopRepo;
	private final CustomerRepository customerRepo;
	private final ShopAccessService shopAccessService;
	private final AccountLedgerService accountLedger;
	private final AuditService auditService;
	private final IdempotencyService idempotencyService;
	private final DebtService debtService;

	public SaleService(SaleRepository saleRepo, ProductRepository productRepo, ShopRepository shopRepo,
			CustomerRepository customerRepo, ShopAccessService shopAccessService,
			AccountLedgerService accountLedger, AuditService auditService,
			IdempotencyService idempotencyService, DebtService debtService) {
		this.saleRepo = saleRepo;
		this.productRepo = productRepo;
		this.shopRepo = shopRepo;
		this.customerRepo = customerRepo;
		this.shopAccessService = shopAccessService;
		this.accountLedger = accountLedger;
		this.auditService = auditService;
		this.idempotencyService = idempotencyService;
		this.debtService = debtService;
	}

	@Transactional
	public ResponseEntity<ApiResponse<Sale>> processSale(
			SaleRequest request, String idempotencyKey, User actor) {
		idempotencyService.reserve(idempotencyKey, "SALE", request, actor);
		if (request.items() == null || request.items().isEmpty()) {
			return badRequest("Cannot checkout with an empty cart");
		}
		Set<String> productIds = new HashSet<>();
		for (SaleItemRequest item : request.items()) {
			if (!productIds.add(item.productId())) {
				return badRequest("Each product may appear only once in the sale");
			}
		}

		List<String> orderedProductIds = productIds.stream().sorted().toList();
		List<Product> lockedProducts = orderedProductIds.stream()
				.map(id -> productRepo.findByIdForUpdate(id)
						.orElseThrow(() -> new EntityNotFoundException("Product not found")))
				.toList();
		java.util.Map<String, Product> productsById = lockedProducts.stream()
				.collect(java.util.stream.Collectors.toMap(Product::getId, product -> product));

		Shop shop = request.shopId() == null || request.shopId().isBlank()
				? lockedProducts.get(0).getShop()
				: shopRepo.findById(request.shopId())
						.orElseThrow(() -> new EntityNotFoundException("Shop not found"));
		if (shop == null) {
			throw new EntityNotFoundException("A valid shop is required to complete this sale");
		}
		shopAccessService.requireShopAccess(shop.getId(), actor);

		BigDecimal calculatedTotal = BigDecimal.ZERO;
		for (SaleItemRequest item : request.items()) {
			Product product = productsById.get(item.productId());
			if (product.getShop() == null || !shop.getId().equals(product.getShop().getId())) {
				return badRequest("All sale products must belong to the selected shop");
			}
			if (!product.isActive()) {
				return badRequest("Product '" + product.getName() + "' is inactive");
			}
			if (!Float.isFinite(item.quantity()) || item.quantity() <= 0
					|| product.getQuantity() < item.quantity()) {
				return badRequest("Insufficient stock for '" + product.getName()
						+ "'. Available: " + product.getQuantity());
			}
			if (product.getSellingPrice() == null || product.getBuyingPrice() == null
					|| product.getSellingPrice().signum() < 0 || product.getBuyingPrice().signum() < 0) {
				return badRequest("Product pricing is invalid for '" + product.getName() + "'");
			}
			if (item.unitPrice() == null || item.unitPrice().compareTo(product.getSellingPrice()) != 0) {
				return badRequest("Price changed for '" + product.getName() + "'. Refresh the cart before checkout");
			}
			calculatedTotal = calculatedTotal.add(product.getSellingPrice()
					.multiply(BigDecimal.valueOf(item.quantity())));
		}

		if (request.grandTotal() == null || request.grandTotal().compareTo(calculatedTotal) != 0) {
			return badRequest("Sale total does not match the current product prices");
		}
		String paymentMethod = request.paymentMethod() == null ? "CASH" : request.paymentMethod().toUpperCase();
		boolean onCredit = "DEBT".equals(paymentMethod);
		if (!Set.of("CASH", "MPESA", "CARD", "BANK_TRANSFER", "DEBT").contains(paymentMethod)) {
			return badRequest("Unsupported payment method");
		}
		BigDecimal tendered = request.amountTendered() == null ? BigDecimal.ZERO : request.amountTendered();
		if (onCredit && tendered.signum() != 0) {
			return badRequest("A debt sale cannot include a payment amount");
		}
		if (!onCredit && tendered.compareTo(calculatedTotal) < 0) {
			return badRequest("Amount tendered is less than the sale total");
		}
		if (!onCredit && !"CASH".equals(paymentMethod) && tendered.compareTo(calculatedTotal) != 0) {
			return badRequest("Non-cash payments must equal the sale total");
		}

		Customer customer = null;
		if (request.customerId() != null && !request.customerId().isBlank()) {
			customer = customerRepo.findById(request.customerId())
					.orElseThrow(() -> new EntityNotFoundException("Customer not found"));
			if (!shop.getId().equals(customer.getShop().getId())) {
				throw new EntityNotFoundException("Customer not found in this shop");
			}
		}
		if (onCredit && customer == null) {
			return badRequest("Select a customer to record this sale as a debt");
		}
		if (onCredit && !customer.isCreditEligible()) {
			return badRequest("This customer is not approved for credit");
		}
		accountLedger.initialize(shop);
		String receiptNumber = "REC-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyMMdd"))
				+ "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
		Sale sale = new Sale(UUID.randomUUID().toString(), receiptNumber, calculatedTotal, tendered,
				tendered.subtract(calculatedTotal), paymentMethod, shop, actor);
		sale.setCustomer(customer);

		for (SaleItemRequest item : request.items()) {
			Product product = productsById.get(item.productId());
			product.setQuantity(product.getQuantity() - item.quantity());
			productRepo.save(product);
			BigDecimal subtotal = product.getSellingPrice().multiply(BigDecimal.valueOf(item.quantity()));
			sale.addItem(new SaleItem(product, product.getName(), item.quantity(),
					product.getSellingPrice(), subtotal, product.getBuyingPrice()));
		}

		Sale savedSale = saleRepo.save(sale);
		if (onCredit) {
			debtService.createForSale(savedSale, customer, actor);
		} else {
			accountLedger.post(shop, "SALE_RECEIPT", "IN", calculatedTotal, paymentMethod,
					"Sale receipt " + receiptNumber, "SALE", savedSale.getId(), actor);
		}
		auditService.record(shop.getId(), actor, "SALE_RECORDED", "SALE", savedSale.getId(),
				"receiptNumber=" + receiptNumber + ", total=" + calculatedTotal);
		idempotencyService.complete(idempotencyKey, savedSale.getId());
		return ResponseEntity.status(201).body(new ApiResponse<>(
				savedSale, "Sale completed successfully! Receipt #" + receiptNumber, true, Instant.now()));
	}

	@Transactional(readOnly = true)
	public ResponseEntity<ApiResponse<List<Sale>>> getAllSales(String shopId, User actor) {
		List<Sale> sales;
		if (shopId != null && !shopId.isBlank() && !"all".equalsIgnoreCase(shopId)) {
			shopAccessService.requireShopAccess(shopId, actor);
			sales = saleRepo.findByShopIdOrderByCreatedAtDesc(shopId);
		} else {
			sales = shopAccessService.getAccessibleShops(actor).stream()
					.flatMap(shop -> saleRepo.findByShopIdOrderByCreatedAtDesc(shop.getId()).stream()).toList();
		}
		List<Sale> newestFirst = sales.stream()
				.sorted(Comparator.comparing(Sale::getCreatedAt,
						Comparator.nullsLast(Comparator.reverseOrder()))
						.thenComparing(Sale::getId, Comparator.nullsLast(Comparator.reverseOrder())))
				.toList();
		return ResponseEntity.ok(new ApiResponse<>(newestFirst, "Sales history loaded", true, Instant.now()));
	}

	@Transactional(readOnly = true)
	public ResponseEntity<ApiResponse<Sale>> getSaleById(String id, User actor) {
		Sale sale = saleRepo.findById(id)
				.orElseThrow(() -> new EntityNotFoundException("Sale transaction not found"));
		shopAccessService.requireShopAccess(sale.getShop().getId(), actor);
		return ResponseEntity.ok(new ApiResponse<>(sale, "Sale details", true, Instant.now()));
	}

	private ResponseEntity<ApiResponse<Sale>> badRequest(String message) {
		return ResponseEntity.badRequest().body(new ApiResponse<>(null, message, false, Instant.now()));
	}

}
