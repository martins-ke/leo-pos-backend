package com.leo.pos.productCategories;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RestController;

import com.leo.pos.ApiResponse;
import com.leo.pos.staff.User;

import jakarta.validation.Valid;

@RequestMapping("/product-categories")
@RestController
@PreAuthorize("hasAnyRole('OWNER', 'STAFF')")
public class ProductCategoryController {

	private final ProductCategoryService categoryService;

	public ProductCategoryController(ProductCategoryService categoryService) {
		this.categoryService = categoryService;
	}

	@GetMapping
	public ResponseEntity<ApiResponse<List<ProductCategory>>> getActiveCategories(
			@RequestParam(required = false) String shopId, @AuthenticationPrincipal User actor) {
		return categoryService.getActiveCategories(shopId, actor);
	}

	@GetMapping("/suggestions")
	public ResponseEntity<ApiResponse<List<String>>> getSuggestions() {
		return categoryService.getSuggestions();
	}

	@PostMapping
	@PreAuthorize("hasRole('OWNER')")
	public ResponseEntity<ApiResponse<ProductCategory>> createDefaultCategory(
			@Valid @RequestBody ProductCategoryRequest req, @AuthenticationPrincipal User actor) {
		return categoryService.createCategory(req, null, actor);
	}

	@PostMapping("/{shopId}")
	@PreAuthorize("hasRole('OWNER')")
	public ResponseEntity<ApiResponse<ProductCategory>> createCategory(
			@Valid @RequestBody ProductCategoryRequest req, @PathVariable String shopId,
			@AuthenticationPrincipal User actor) {
		return categoryService.createCategory(req, shopId, actor);
	}

	@PutMapping("/{id}")
	@PreAuthorize("hasRole('OWNER')")
	public ResponseEntity<ApiResponse<ProductCategory>> renameCategory(
			@PathVariable String id, @Valid @RequestBody ProductCategoryRequest req,
			@AuthenticationPrincipal User actor) {
		return categoryService.renameCategory(id, req, actor);
	}

	@DeleteMapping("/{id}")
	@PreAuthorize("hasRole('OWNER')")
	public ResponseEntity<ApiResponse<String>> archiveCategory(
			@PathVariable String id, @AuthenticationPrincipal User actor) {
		return categoryService.archiveCategory(id, actor);
	}

	@PatchMapping("/{id}/restore")
	@PreAuthorize("hasRole('OWNER')")
	public ResponseEntity<ApiResponse<ProductCategory>> restoreCategory(
			@PathVariable String id, @AuthenticationPrincipal User actor) {
		return categoryService.restoreCategory(id, actor);
	}
}
