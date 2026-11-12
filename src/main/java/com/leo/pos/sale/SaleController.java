package com.leo.pos.sale;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import com.leo.pos.ApiResponse;
import com.leo.pos.staff.User;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/sales")
@PreAuthorize("hasAnyRole('OWNER', 'STAFF')")
public class SaleController {

	private final SaleService saleService;

	public SaleController(SaleService saleService) {
		this.saleService = saleService;
	}

	@PostMapping
	public ResponseEntity<ApiResponse<Sale>> createSale(
			@Valid @RequestBody SaleRequest req,
			@RequestHeader("Idempotency-Key") String idempotencyKey,
			@AuthenticationPrincipal User actor) {
		return saleService.processSale(req, idempotencyKey, actor);
	}

	@GetMapping
	public ResponseEntity<ApiResponse<List<Sale>>> getAllSales(
			@RequestParam(required = false) String shopId, @AuthenticationPrincipal User actor) {
		return saleService.getAllSales(shopId, actor);
	}

	@GetMapping("/{id}")
	public ResponseEntity<ApiResponse<Sale>> getSaleById(@PathVariable String id, @AuthenticationPrincipal User actor) {
		return saleService.getSaleById(id, actor);
	}
}
