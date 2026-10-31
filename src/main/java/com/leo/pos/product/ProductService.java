package com.leo.pos.product;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.leo.pos.ApiResponse;
import com.leo.pos.security.ShopAccessService;
import com.leo.pos.shop.Shop;
import com.leo.pos.shop.ShopRepository;
import com.leo.pos.staff.User;

import jakarta.persistence.EntityNotFoundException;

@Service
public class ProductService {

	private final ProductRepository productRepo;
	private final ShopRepository shopRepo;
	private final ShopAccessService shopAccessService;

	public ProductService(ProductRepository productRepo, ShopRepository shopRepo, ShopAccessService shopAccessService) {
		this.productRepo = productRepo;
		this.shopRepo = shopRepo;
		this.shopAccessService = shopAccessService;
	}

	@Transactional
	public ResponseEntity<ApiResponse<Product>> createProduct(ProductRequest req, String shopId, User actor) {
		Shop shop;
		if (shopId == null || shopId.isBlank()) {
			if (!"OWNER".equalsIgnoreCase(actor.getRole())) {
				throw new org.springframework.security.access.AccessDeniedException("Only owners can create products");
			}
			List<Shop> ownedShops = shopRepo.findByUserId(actor.getId());
			if (ownedShops.isEmpty()) {
				throw new IllegalArgumentException("Create a shop before adding products");
			}
			shop = ownedShops.get(0);
		} else {
			shop = shopAccessService.requireOwnerShop(shopId, actor);
		}
		String name = req.name().trim();
		if (productRepo.findByNameAndShopId(name, shop.getId()).isPresent()) {
			return ResponseEntity.badRequest().body(new ApiResponse<>(null,
					"Failed! Product with name '" + name + "' already exists in this shop.", false, Instant.now()));
		}
		Product product = new Product(UUID.randomUUID().toString(), name, req.buyingPrice(), req.sellingPrice(),
				req.quantityInStock(), req.minStockLevel(), req.unit().trim(), true, shop);
		return ResponseEntity.status(201).body(new ApiResponse<>(
				productRepo.save(product), "Success, Product created", true, Instant.now()));
	}

	@Transactional(readOnly = true)
	public ResponseEntity<ApiResponse<List<Product>>> getActiveProducts(String shopId, User actor) {
		List<Product> products = new ArrayList<>();
		if (shopId != null && !shopId.isBlank() && !"all".equalsIgnoreCase(shopId)) {
			shopAccessService.requireShopAccess(shopId, actor);
			products.addAll(productRepo.findByShopIdAndActiveTrue(shopId));
		} else {
			String ownerId = "OWNER".equalsIgnoreCase(actor.getRole()) ? actor.getId() : actor.getCreatedBy();
			for (Shop shop : shopRepo.findByUserId(ownerId)) {
				products.addAll(productRepo.findByShopIdAndActiveTrue(shop.getId()));
			}
		}
		return ResponseEntity.ok(new ApiResponse<>(products, "Products & Inventory", true, Instant.now()));
	}

	@Transactional(readOnly = true)
	public ResponseEntity<ApiResponse<Product>> getProductById(String id, User actor) {
		Product product = productRepo.findById(id)
				.orElseThrow(() -> new EntityNotFoundException("Product not found"));
		if (product.getShop() == null) {
			throw new EntityNotFoundException("Product not found");
		}
		shopAccessService.requireShopAccess(product.getShop().getId(), actor);
		return ResponseEntity.ok(new ApiResponse<>(product, "Product details", true, Instant.now()));
	}

	@Transactional
	public ResponseEntity<ApiResponse<Product>> updateProduct(String id, ProductRequest req, User actor) {
		Product product = productRepo.findByIdForUpdate(id)
				.orElseThrow(() -> new EntityNotFoundException("Product not found"));
		shopAccessService.requireOwnerShop(product.getShop().getId(), actor);
		product.setName(req.name().trim());
		product.setBuyingPrice(req.buyingPrice());
		product.setSellingPrice(req.sellingPrice());
		product.setQuantity(req.quantityInStock());
		product.setMinStockLevel(req.minStockLevel());
		product.setUnit(req.unit().trim());
		return ResponseEntity.ok(new ApiResponse<>(productRepo.save(product), "Product updated successfully", true,
				Instant.now()));
	}

	@Transactional
	public ResponseEntity<ApiResponse<Product>> restockProduct(String id, float additionalQuantity, User actor) {
		if (!Float.isFinite(additionalQuantity) || additionalQuantity <= 0) {
			throw new IllegalArgumentException("Restock quantity must be a finite positive number");
		}
		Product product = productRepo.findByIdForUpdate(id)
				.orElseThrow(() -> new EntityNotFoundException("Product not found"));
		shopAccessService.requireOwnerShop(product.getShop().getId(), actor);
		product.setQuantity(product.getQuantity() + additionalQuantity);
		product.setActive(true);
		Product saved = productRepo.save(product);
		return ResponseEntity.ok(new ApiResponse<>(saved,
				"Product restocked as a quantity-only adjustment. Record a stock purchase to update the shop account.",
				true, Instant.now()));
	}

	@Transactional
	public ResponseEntity<ApiResponse<String>> deleteProduct(String id, User actor) {
		Product product = productRepo.findByIdForUpdate(id)
				.orElseThrow(() -> new EntityNotFoundException("Product not found"));
		shopAccessService.requireOwnerShop(product.getShop().getId(), actor);
		product.setActive(false);
		productRepo.save(product);
		return ResponseEntity.ok(new ApiResponse<>("Product deactivated successfully",
				"Success", true, Instant.now()));
	}
}
