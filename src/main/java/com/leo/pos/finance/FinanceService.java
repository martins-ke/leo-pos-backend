package com.leo.pos.finance;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.leo.pos.audit.AuditService;
import com.leo.pos.product.Product;
import com.leo.pos.product.ProductRepository;
import com.leo.pos.sale.SaleRepository;
import com.leo.pos.security.ShopAccessService;
import com.leo.pos.shop.Shop;
import com.leo.pos.staff.User;

import jakarta.persistence.EntityNotFoundException;

@Service
public class FinanceService {

	private final ExpenseRepository expenseRepository;
	private final ExpenseCategoryRepository categoryRepository;
	private final StockWithdrawalRepository withdrawalRepository;
	private final StockPurchaseRepository purchaseRepository;
	private final ShopAccountRepository accountRepository;
	private final AccountTransactionRepository transactionRepository;
	private final ProductRepository productRepository;
	private final SaleRepository saleRepository;
	private final ShopAccessService shopAccessService;
	private final AccountLedgerService accountLedger;
	private final AuditService auditService;
	private final IdempotencyService idempotencyService;

	public FinanceService(ExpenseRepository expenseRepository, ExpenseCategoryRepository categoryRepository,
			StockWithdrawalRepository withdrawalRepository, StockPurchaseRepository purchaseRepository,
			ShopAccountRepository accountRepository, AccountTransactionRepository transactionRepository,
			ProductRepository productRepository, SaleRepository saleRepository,
			ShopAccessService shopAccessService, AccountLedgerService accountLedger, AuditService auditService,
			IdempotencyService idempotencyService) {
		this.expenseRepository = expenseRepository;
		this.categoryRepository = categoryRepository;
		this.withdrawalRepository = withdrawalRepository;
		this.purchaseRepository = purchaseRepository;
		this.accountRepository = accountRepository;
		this.transactionRepository = transactionRepository;
		this.productRepository = productRepository;
		this.saleRepository = saleRepository;
		this.shopAccessService = shopAccessService;
		this.accountLedger = accountLedger;
		this.auditService = auditService;
		this.idempotencyService = idempotencyService;
	}

	@Transactional(readOnly = true)
	public Page<Expense> listExpenses(String shopId, int page, int size, User actor) {
		shopAccessService.requireOwnerShop(shopId, actor);
		return expenseRepository.findByShopIdOrderByExpenseDateDescCreatedAtDesc(
				shopId, PageRequest.of(page, Math.min(size, 100)));
	}

	@Transactional(readOnly = true)
	public BigDecimal dailyExpenseTotal(String shopId, User actor) {
		shopAccessService.requireOwnerShop(shopId, actor);
		LocalDate today = LocalDate.now();
		return expenseRepository.sumByShopAndPeriod(shopId, today, today);
	}

	@Transactional(readOnly = true)
	public BigDecimal dailyStockWithdrawalTotal(String shopId, User actor) {
		shopAccessService.requireOwnerShop(shopId, actor);
		LocalDate today = LocalDate.now();
		ZoneId zone = ZoneId.systemDefault();
		return withdrawalRepository.sumByShopAndOccurredAt(shopId,
				today.atStartOfDay(zone).toInstant(), today.plusDays(1).atStartOfDay(zone).toInstant());
	}

	@Transactional
	public Expense createExpense(ExpenseRequest request, String idempotencyKey, User actor) {
		idempotencyService.reserve(idempotencyKey, "EXPENSE", request, actor);
		Shop shop = shopAccessService.requireOwnerShop(request.shopId(), actor);
		accountLedger.initialize(shop);
		if (request.expenseDate().isAfter(LocalDate.now())) {
			throw new IllegalArgumentException("Expense date cannot be in the future");
		}
		String categoryName = request.category().trim();
		ensureCategory(shop, categoryName);
		String paymentMethod = request.paymentMethod().toUpperCase();
		Expense expense = expenseRepository.save(new Expense(
				UUID.randomUUID().toString(), shop, categoryName, request.amount(),
				request.expenseDate(), normalize(request.payee()), paymentMethod,
				normalize(request.notes()), true, null, null, actor.getId(), actor.getUsername()));
		accountLedger.post(shop, "OPERATING_EXPENSE", "OUT", request.amount(), paymentMethod,
				"Expense: " + categoryName, "EXPENSE", expense.getId(), actor);
		auditService.record(shop.getId(), actor, "EXPENSE_RECORDED", "EXPENSE", expense.getId(),
				"category=" + categoryName + ", amount=" + request.amount());
		idempotencyService.complete(idempotencyKey, expense.getId());
		return expense;
	}

	@Transactional(readOnly = true)
	public List<ExpenseCategory> listCategories(String shopId, User actor) {
		shopAccessService.requireOwnerShop(shopId, actor);
		return categoryRepository.findByShopIdOrderByNameAsc(shopId);
	}

	@Transactional
	public ExpenseCategory createCategory(ExpenseCategoryRequest request, String idempotencyKey, User actor) {
		idempotencyService.reserve(idempotencyKey, "EXPENSE_CATEGORY", request, actor);
		Shop shop = shopAccessService.requireOwnerShop(request.shopId(), actor);
		String name = request.name().trim();
		if (name.isBlank() || name.length() > 80) {
			throw new IllegalArgumentException("Category must contain 1 to 80 characters");
		}
		ExpenseCategory category = ensureCategory(shop, name);
		idempotencyService.complete(idempotencyKey, category.getId());
		return category;
	}

	@Transactional
	public StockWithdrawal withdrawStock(StockWithdrawalRequest request, String idempotencyKey, User actor) {
		idempotencyService.reserve(idempotencyKey, "STOCK_WITHDRAWAL", request, actor);
		Shop shop = shopAccessService.requireOwnerShop(request.shopId(), actor);
		requireFiniteQuantity(request.quantity());
		accountLedger.initialize(shop);
		Product product = productRepository.findByIdForUpdate(request.productId())
				.orElseThrow(() -> new EntityNotFoundException("Product not found"));
		if (product.getShop() == null || !shop.getId().equals(product.getShop().getId())) {
			throw new EntityNotFoundException("Product not found in this shop");
		}
		if (product.getQuantity() < request.quantity()) {
			throw new IllegalArgumentException("Withdrawal quantity exceeds available stock");
		}
		if (product.getBuyingPrice() == null || product.getBuyingPrice().signum() < 0) {
			throw new IllegalArgumentException("Product must have a valid buying price before stock can be withdrawn");
		}
		BigDecimal unitCost = product.getBuyingPrice();
		BigDecimal cost = unitCost.multiply(BigDecimal.valueOf(request.quantity()))
				.setScale(2, RoundingMode.HALF_UP);
		String id = UUID.randomUUID().toString();
		product.setQuantity(product.getQuantity() - request.quantity());
		productRepository.save(product);
		StockWithdrawal withdrawal = withdrawalRepository.save(new StockWithdrawal(
				id, shop, product, request.quantity(), unitCost, cost,
				request.reason().trim(), actor.getId(), actor.getUsername()));
		ensureCategory(shop, "Personal Use");
		expenseRepository.save(new Expense(
				UUID.randomUUID().toString(), shop, "Personal Use", cost, LocalDate.now(),
				request.reason().trim(), null, "Stock withdrawn for personal use", false,
				"STOCK_WITHDRAWAL", withdrawal.getId(), actor.getId(), actor.getUsername()));
		auditService.record(shop.getId(), actor, "STOCK_WITHDRAWN", "STOCK_WITHDRAWAL", id,
				"productId=" + product.getId() + ", quantity=" + request.quantity() + ", cost=" + cost);
		idempotencyService.complete(idempotencyKey, id);
		return withdrawal;
	}

	@Transactional
	public StockPurchase purchaseStock(StockPurchaseRequest request, String idempotencyKey, User actor) {
		idempotencyService.reserve(idempotencyKey, "STOCK_PURCHASE", request, actor);
		Shop shop = shopAccessService.requireOwnerShop(request.shopId(), actor);
		requireFiniteQuantity(request.quantity());
		Product product = productRepository.findByIdForUpdate(request.productId())
				.orElseThrow(() -> new EntityNotFoundException("Product not found"));
		if (product.getShop() == null || !shop.getId().equals(product.getShop().getId())) {
			throw new EntityNotFoundException("Product not found in this shop");
		}
		BigDecimal totalCost = request.unitCost().multiply(BigDecimal.valueOf(request.quantity()))
				.setScale(2, RoundingMode.HALF_UP);
		if (totalCost.signum() <= 0) {
			throw new IllegalArgumentException("Purchase total must be greater than zero");
		}
		BigDecimal available = accountLedger.balanceForUpdate(shop);
		if (available.compareTo(totalCost) < 0) {
			throw new IllegalArgumentException("Insufficient shop-account balance for this purchase");
		}
		product.setQuantity(product.getQuantity() + request.quantity());
		product.setBuyingPrice(request.unitCost());
		productRepository.save(product);
		StockPurchase purchase = purchaseRepository.save(new StockPurchase(
				UUID.randomUUID().toString(), shop, product, request.quantity(), request.unitCost(),
				totalCost, normalize(request.notes()), actor.getId(), actor.getUsername()));
		String paymentMethod = normalize(request.paymentMethod());
		if (paymentMethod == null) paymentMethod = "CASH";
		accountLedger.post(shop, "STOCK_PURCHASE", "OUT", totalCost, paymentMethod,
				"Stock purchase: " + product.getName(), "STOCK_PURCHASE", purchase.getId(), actor);
		auditService.record(shop.getId(), actor, "STOCK_PURCHASE_RECORDED",
				"STOCK_PURCHASE", purchase.getId(), "productId=" + product.getId() + ", total=" + totalCost);
		idempotencyService.complete(idempotencyKey, purchase.getId());
		return purchase;
	}

	@Transactional
	public AccountTransaction addCapital(String shopId, CapitalRequest request, String idempotencyKey, User actor) {
		idempotencyService.reserve(idempotencyKey, "CAPITAL", shopId + "|" + request, actor);
		Shop shop = shopAccessService.requireOwnerShop(shopId, actor);
		AccountTransaction transaction = accountLedger.post(
				shop, "CAPITAL", "IN", request.amount(), normalizeUpper(request.paymentMethod()),
				normalize(request.description()) == null ? "Owner capital added" : request.description().trim(),
				null, null, actor);
		auditService.record(shopId, actor, "CAPITAL_ADDED", "ACCOUNT_TRANSACTION",
				transaction.getId(), "amount=" + request.amount());
		idempotencyService.complete(idempotencyKey, transaction.getId());
		return transaction;
	}

	@Transactional
	public AccountTransaction withdraw(String shopId, DrawingRequest request, String idempotencyKey, User actor) {
		idempotencyService.reserve(idempotencyKey, "OWNER_DRAWING", shopId + "|" + request, actor);
		Shop shop = shopAccessService.requireOwnerShop(shopId, actor);
		BigDecimal cashBalance = accountLedger.balanceForUpdate(shop);
		if (cashBalance.compareTo(request.amount()) < 0) {
			throw new IllegalArgumentException("Drawing exceeds available shop-account cash");
		}
		AccountSummary summary = summaryForShop(shop);
		if (request.amount().compareTo(summary.remainingProfitAfterDrawings()) > 0
				&& !request.confirmOverProfit()) {
			throw new ResponseStatusException(HttpStatus.CONFLICT,
					"Drawing exceeds remaining profit. Confirm to use working capital.");
		}
		AccountTransaction transaction = accountLedger.post(
				shop, "OWNER_DRAWING", "OUT", request.amount(), normalizeUpper(request.paymentMethod()),
				normalize(request.description()) == null ? "Owner drawing" : request.description().trim(),
				null, null, actor);
		auditService.record(shopId, actor, "OWNER_DRAWING_RECORDED", "ACCOUNT_TRANSACTION",
				transaction.getId(), "amount=" + request.amount());
		idempotencyService.complete(idempotencyKey, transaction.getId());
		return transaction;
	}

	@Transactional(readOnly = true)
	public AccountSummary accountSummary(String shopId, User actor) {
		Shop shop = shopAccessService.requireOwnerShop(shopId, actor);
		return summaryForShop(shop);
	}

	@Transactional(readOnly = true)
	public Page<AccountTransactionView> transactions(String shopId, int page, int size, User actor) {
		Shop shop = shopAccessService.requireOwnerShop(shopId, actor);
		return accountRepository.findByShopId(shop.getId())
				.map(account -> transactionRepository
						.findByAccountIdOrderByOccurredAtDesc(account.getId(),
								PageRequest.of(page, Math.min(size, 100)))
						.map(AccountTransactionView::from))
				.orElseGet(() -> Page.empty(PageRequest.of(page, Math.min(size, 100))));
	}

	@Transactional(readOnly = true)
	public Page<StockWithdrawal> withdrawals(String shopId, int page, int size, User actor) {
		shopAccessService.requireOwnerShop(shopId, actor);
		return withdrawalRepository.findByShopIdOrderByOccurredAtDesc(
				shopId, PageRequest.of(page, Math.min(size, 100)));
	}

	@Transactional(readOnly = true)
	public Page<StockPurchase> purchases(String shopId, int page, int size, User actor) {
		shopAccessService.requireOwnerShop(shopId, actor);
		return purchaseRepository.findByShopIdOrderByOccurredAtDesc(
				shopId, PageRequest.of(page, Math.min(size, 100)));
	}

	private AccountSummary summaryForShop(Shop shop) {
		ShopAccount account = accountRepository.findByShopId(shop.getId()).orElse(null);
		BigDecimal cashBalance = account == null
				? BigDecimal.ZERO
				: transactionRepository.calculateBalance(account.getId());
		BigDecimal grossProfit = saleRepository.sumGrossProfitByShopId(shop.getId());
		BigDecimal expenses = expenseRepository.sumByShopId(shop.getId());
		BigDecimal netProfit = grossProfit.subtract(expenses);
		BigDecimal drawings = transactionRepository.calculateTotalDrawingsByShopId(shop.getId());
		return new AccountSummary(shop.getId(), cashBalance, grossProfit, expenses,
				netProfit, drawings, netProfit.subtract(drawings));
	}

	private ExpenseCategory ensureCategory(Shop shop, String name) {
		return categoryRepository.findByShopIdAndNameIgnoreCase(shop.getId(), name)
				.orElseGet(() -> categoryRepository.save(
						new ExpenseCategory(UUID.randomUUID().toString(), shop, name)));
	}

	private void requireFiniteQuantity(Float quantity) {
		if (quantity == null || !Float.isFinite(quantity) || quantity <= 0) {
			throw new IllegalArgumentException("Quantity must be a finite positive number");
		}
	}

	private String normalize(String value) {
		return value == null || value.isBlank() ? null : value.trim();
	}

	private String normalizeUpper(String value) {
		String normalized = normalize(value);
		return normalized == null ? "CASH" : normalized.toUpperCase();
	}
}
