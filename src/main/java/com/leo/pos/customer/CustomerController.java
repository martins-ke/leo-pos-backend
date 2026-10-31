package com.leo.pos.customer;

import java.time.Instant;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
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
@RequestMapping("/customers")
@PreAuthorize("hasAnyRole('OWNER', 'STAFF')")
public class CustomerController {

	private final CustomerService customerService;

	public CustomerController(CustomerService customerService) {
		this.customerService = customerService;
	}

	@GetMapping
	public ResponseEntity<ApiResponse<Page<CustomerView>>> list(
			@RequestParam String shopId,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "50") int size,
			@AuthenticationPrincipal User actor) {
		return ResponseEntity.ok(new ApiResponse<>(
				customerService.list(shopId, Math.max(0, page), Math.max(1, size), actor),
				"Customers loaded", true, Instant.now()));
	}

	@PostMapping
	public ResponseEntity<ApiResponse<CustomerView>> create(
			@Valid @RequestBody CustomerRequest request,
			@RequestHeader("Idempotency-Key") String idempotencyKey,
			@AuthenticationPrincipal User actor) {
		return ResponseEntity.status(201).body(new ApiResponse<>(
				customerService.create(request, idempotencyKey, actor), "Customer created", true, Instant.now()));
	}

	@PatchMapping("/{id}/eligibility")
	@PreAuthorize("hasRole('OWNER')")
	public ResponseEntity<ApiResponse<CustomerView>> setEligibility(
			@PathVariable String id, @RequestBody EligibilityRequest request,
			@AuthenticationPrincipal User actor) {
		return ResponseEntity.ok(new ApiResponse<>(
				customerService.setEligibility(id, request.creditEligible(), actor),
				"Credit eligibility updated", true, Instant.now()));
	}

	@DeleteMapping("/{id}")
	@PreAuthorize("hasRole('OWNER')")
	public ResponseEntity<ApiResponse<String>> delete(
			@PathVariable String id, @AuthenticationPrincipal User actor) {
		customerService.delete(id, actor);
		return ResponseEntity.ok(new ApiResponse<>(id, "Customer deleted and historical sales anonymized",
				true, Instant.now()));
	}
}
