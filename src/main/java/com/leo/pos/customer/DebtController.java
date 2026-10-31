package com.leo.pos.customer;

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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import com.leo.pos.ApiResponse;
import com.leo.pos.staff.User;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/debts")
@PreAuthorize("hasAnyRole('OWNER', 'STAFF')")
public class DebtController {

	private final DebtService debtService;

	public DebtController(DebtService debtService) {
		this.debtService = debtService;
	}

	@GetMapping
	public ResponseEntity<ApiResponse<Page<DebtView>>> list(
			@RequestParam String shopId,
			@RequestParam(defaultValue = "OPEN") String status,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "50") int size,
			@AuthenticationPrincipal User actor) {
		return ResponseEntity.ok(new ApiResponse<>(
				debtService.list(shopId, status, Math.max(0, page), Math.max(1, size), actor),
				"Customer debts loaded", true, Instant.now()));
	}

	@PostMapping
	public ResponseEntity<ApiResponse<DebtView>> create(
			@Valid @RequestBody DebtRequest request,
			@RequestHeader("Idempotency-Key") String idempotencyKey,
			@AuthenticationPrincipal User actor) {
		return ResponseEntity.status(201).body(new ApiResponse<>(
				debtService.create(request, idempotencyKey, actor), "Debt recorded", true, Instant.now()));
	}

	@PostMapping("/{id}/payments")
	public ResponseEntity<ApiResponse<DebtView>> recordPayment(
			@PathVariable String id, @Valid @RequestBody DebtPaymentRequest request,
			@RequestHeader("Idempotency-Key") String idempotencyKey,
			@AuthenticationPrincipal User actor) {
		return ResponseEntity.ok(new ApiResponse<>(
				debtService.recordPayment(id, request, idempotencyKey, actor), "Debt payment recorded", true, Instant.now()));
	}

	@GetMapping("/{id}/payments")
	public ResponseEntity<ApiResponse<List<DebtPayment>>> payments(
			@PathVariable String id, @AuthenticationPrincipal User actor) {
		return ResponseEntity.ok(new ApiResponse<>(
				debtService.paymentHistory(id, actor), "Debt payment history loaded", true, Instant.now()));
	}
}
