package com.leo.pos.product;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import com.leo.pos.ApiResponse;
import com.leo.pos.staff.User;

import jakarta.validation.Valid;

@RequestMapping("/products")
@RestController
@CrossOrigin(origins = "http://localhost:5173")
@PreAuthorize("hasAnyRole('OWNER', 'STAFF')")
public class ProductController {
	private final ProductService productService;

	public ProductController(ProductService productService) {
		this.productService = productService;
	}
	
	@PostMapping
	@PreAuthorize("hasRole('OWNER')")
	public ResponseEntity<ApiResponse<Product>> createProductDefault(
			@Valid @RequestBody ProductRequest req, @AuthenticationPrincipal User actor){
		return productService.createProduct(req, null, actor); 
	}

	@PostMapping("/{shopId}")
	@PreAuthorize("hasRole('OWNER')")
	public ResponseEntity<ApiResponse<Product>> createProduct(
			@Valid @RequestBody ProductRequest req, @PathVariable String shopId, @AuthenticationPrincipal User actor){
		return productService.createProduct(req, shopId, actor); 
	}

	@GetMapping
	public ResponseEntity<ApiResponse<List<Product>>> getAllActiveProducts(
			@org.springframework.web.bind.annotation.RequestParam(required = false) String shopId,
			@AuthenticationPrincipal User actor){
		return productService.getActiveProducts(shopId, actor);   
	}
	
	@GetMapping("/{shopId}")
	public ResponseEntity<ApiResponse<List<Product>>> getActiveProducts(
			@PathVariable String shopId, @AuthenticationPrincipal User actor){
		return productService.getActiveProducts(shopId, actor);   
	}

	@GetMapping("/detail/{id}")
	public ResponseEntity<ApiResponse<Product>> getProductById(
			@PathVariable String id, @AuthenticationPrincipal User actor){
		return productService.getProductById(id, actor);
	}

	@org.springframework.web.bind.annotation.PutMapping("/{id}")
	@PreAuthorize("hasRole('OWNER')")
	public ResponseEntity<ApiResponse<Product>> updateProduct(
			@PathVariable String id, @Valid @RequestBody ProductRequest req, @AuthenticationPrincipal User actor){
		return productService.updateProduct(id, req, actor);
	}

	@org.springframework.web.bind.annotation.PatchMapping("/{id}/stock")
	@PreAuthorize("hasRole('OWNER')")
	public ResponseEntity<ApiResponse<Product>> restockProduct(
			@PathVariable String id, @org.springframework.web.bind.annotation.RequestParam float quantity,
			@AuthenticationPrincipal User actor){
		return productService.restockProduct(id, quantity, actor);
	}

	@org.springframework.web.bind.annotation.DeleteMapping("/{id}")
	@PreAuthorize("hasRole('OWNER')")
	public ResponseEntity<ApiResponse<String>> deleteProduct(
			@PathVariable String id, @AuthenticationPrincipal User actor){
		return productService.deleteProduct(id, actor);
	}
}
