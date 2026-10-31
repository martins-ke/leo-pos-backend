package com.leo.pos.finance;

import java.time.Instant;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import com.leo.pos.ApiResponse;
import com.leo.pos.staff.User;

import jakarta.validation.Valid;

@RestController
@PreAuthorize("hasRole('OWNER')")
public class FinanceController {

	private final FinanceService financeService;

	public FinanceController(FinanceService financeService) {
		this.financeService = financeService;
	}

	@GetMapping("/expenses")
	public ResponseEntity<ApiResponse<Page<Expense>>> expenses(
			@RequestParam String shopId,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "50") int size,
			@AuthenticationPrincipal User actor) {
		return ResponseEntity.ok(new ApiResponse<>(
				financeService.listExpenses(shopId, Math.max(0, page), Math.max(1, size), actor),
				"Expenses loaded", true, Instant.now()));
	}

	@PostMapping("/expenses")
	public ResponseEntity<ApiResponse<Expense>> createExpense(
			@Valid @RequestBody ExpenseRequest request,
			@RequestHeader("Idempotency-Key") String idempotencyKey,
			@AuthenticationPrincipal User actor) {
		return ResponseEntity.status(201).body(new ApiResponse<>(
				financeService.createExpense(request, idempotencyKey, actor), "Expense recorded", true, Instant.now()));
	}

	@GetMapping("/expenses/categories")
	public ResponseEntity<ApiResponse<List<ExpenseCategory>>> categories(
			@RequestParam String shopId, @AuthenticationPrincipal User actor) {
		return ResponseEntity.ok(new ApiResponse<>(
				financeService.listCategories(shopId, actor), "Expense categories loaded", true, Instant.now()));
	}

	@PostMapping("/expenses/categories")
	public ResponseEntity<ApiResponse<ExpenseCategory>> createCategory(
			@Valid @RequestBody ExpenseCategoryRequest request,
			@RequestHeader("Idempotency-Key") String idempotencyKey,
			@AuthenticationPrincipal User actor) {
		return ResponseEntity.status(201).body(new ApiResponse<>(
				financeService.createCategory(request, idempotencyKey, actor), "Expense category saved", true, Instant.now()));
	}

	@PostMapping("/inventory/withdrawals")
	public ResponseEntity<ApiResponse<StockWithdrawal>> withdrawStock(
			@Valid @RequestBody StockWithdrawalRequest request,
			@RequestHeader("Idempotency-Key") String idempotencyKey,
			@AuthenticationPrincipal User actor) {
		return ResponseEntity.status(201).body(new ApiResponse<>(
				financeService.withdrawStock(request, idempotencyKey, actor), "Stock withdrawal recorded", true, Instant.now()));
	}

	@GetMapping("/inventory/withdrawals")
	public ResponseEntity<ApiResponse<Page<StockWithdrawal>>> withdrawals(
			@RequestParam String shopId,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "50") int size,
			@AuthenticationPrincipal User actor) {
		return ResponseEntity.ok(new ApiResponse<>(
				financeService.withdrawals(shopId, Math.max(0, page), Math.max(1, size), actor),
				"Stock withdrawals loaded", true, Instant.now()));
	}

	@PostMapping("/inventory/purchases")
	public ResponseEntity<ApiResponse<StockPurchase>> purchaseStock(
			@Valid @RequestBody StockPurchaseRequest request,
			@RequestHeader("Idempotency-Key") String idempotencyKey,
			@AuthenticationPrincipal User actor) {
		return ResponseEntity.status(201).body(new ApiResponse<>(
				financeService.purchaseStock(request, idempotencyKey, actor), "Stock purchase recorded", true, Instant.now()));
	}

	@GetMapping("/inventory/purchases")
	public ResponseEntity<ApiResponse<Page<StockPurchase>>> purchases(
			@RequestParam String shopId,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "50") int size,
			@AuthenticationPrincipal User actor) {
		return ResponseEntity.ok(new ApiResponse<>(
				financeService.purchases(shopId, Math.max(0, page), Math.max(1, size), actor),
				"Stock purchases loaded", true, Instant.now()));
	}

	@GetMapping("/accounts/{shopId}/summary")
	public ResponseEntity<ApiResponse<AccountSummary>> accountSummary(
			@PathVariable String shopId, @AuthenticationPrincipal User actor) {
		return ResponseEntity.ok(new ApiResponse<>(
				financeService.accountSummary(shopId, actor), "Shop account summary loaded", true, Instant.now()));
	}

	@GetMapping("/accounts/{shopId}/transactions")
	public ResponseEntity<ApiResponse<Page<AccountTransactionView>>> transactions(
			@PathVariable String shopId,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "50") int size,
			@AuthenticationPrincipal User actor) {
		return ResponseEntity.ok(new ApiResponse<>(
				financeService.transactions(shopId, Math.max(0, page), Math.max(1, size), actor),
				"Shop account transactions loaded", true, Instant.now()));
	}

	@PostMapping("/accounts/{shopId}/capital")
	public ResponseEntity<ApiResponse<AccountTransaction>> addCapital(
			@PathVariable String shopId, @Valid @RequestBody CapitalRequest request,
			@RequestHeader("Idempotency-Key") String idempotencyKey,
			@AuthenticationPrincipal User actor) {
		return ResponseEntity.status(201).body(new ApiResponse<>(
				financeService.addCapital(shopId, request, idempotencyKey, actor), "Capital recorded", true, Instant.now()));
	}

	@PostMapping("/accounts/{shopId}/drawings")
	public ResponseEntity<ApiResponse<AccountTransaction>> withdraw(
			@PathVariable String shopId, @Valid @RequestBody DrawingRequest request,
			@RequestHeader("Idempotency-Key") String idempotencyKey,
			@AuthenticationPrincipal User actor) {
		return ResponseEntity.status(201).body(new ApiResponse<>(
				financeService.withdraw(shopId, request, idempotencyKey, actor), "Owner drawing recorded", true, Instant.now()));
	}
}
