package com.leo.pos.productCategories;

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
public class ProductCategoryService {

	private static final List<String> SUGGESTED_CATEGORIES = List.of(
			"Beverages", "Bakery", "Dairy", "Fresh Produce", "Grains & Cereals",
			"Meat & Seafood", "Snacks", "Frozen Foods", "Household", "Personal Care");

	private final ProductCategoryRepository categoryRepo;
	private final ShopRepository shopRepo;
	private final ShopAccessService shopAccessService;

	public ProductCategoryService(ProductCategoryRepository categoryRepo, ShopRepository shopRepo,
			ShopAccessService shopAccessService) {
		this.categoryRepo = categoryRepo;
		this.shopRepo = shopRepo;
		this.shopAccessService = shopAccessService;
	}

	@Transactional(readOnly = true)
	public ResponseEntity<ApiResponse<List<ProductCategory>>> getActiveCategories(String shopId, User actor) {
		List<ProductCategory> categories = new ArrayList<>();
		if (shopId != null && !shopId.isBlank() && !"all".equalsIgnoreCase(shopId)) {
			shopAccessService.requireShopAccess(shopId, actor);
			categories.addAll(categoryRepo.findByShop_IdAndActiveTrueOrderByNameAsc(shopId));
		} else {
			for (Shop shop : shopAccessService.getAccessibleShops(actor)) {
				categories.addAll(categoryRepo.findByShop_IdAndActiveTrueOrderByNameAsc(shop.getId()));
			}
		}
		return ResponseEntity.ok(new ApiResponse<>(categories, "Active product categories", true, Instant.now()));
	}

	@Transactional(readOnly = true)
	public ResponseEntity<ApiResponse<List<String>>> getSuggestions() {
		return ResponseEntity.ok(new ApiResponse<>(SUGGESTED_CATEGORIES, "Suggested product categories", true,
				Instant.now()));
	}

	@Transactional
	public ResponseEntity<ApiResponse<ProductCategory>> createCategory(ProductCategoryRequest req, String shopId,
			User actor) {
		Shop shop;
		if (shopId == null || shopId.isBlank()) {
			if (actor == null || !"OWNER".equalsIgnoreCase(actor.getRole())) {
				throw new org.springframework.security.access.AccessDeniedException(
						"Only owners can create product categories");
			}
			List<Shop> ownedShops = shopRepo.findByUserId(actor.getId());
			if (ownedShops.isEmpty()) {
				throw new IllegalArgumentException("Create a shop before adding product categories");
			}
			shop = ownedShops.get(0);
		} else {
			shop = shopAccessService.requireOwnerShop(shopId, actor);
		}

		String name = req.name().trim();
		if (categoryRepo.findByShop_IdAndNameIgnoreCase(shop.getId(), name).isPresent()) {
			throw new IllegalArgumentException("A product category with this name already exists in this shop.");
		}
		ProductCategory category = new ProductCategory(UUID.randomUUID().toString(), name, shop);
		ProductCategory saved = categoryRepo.save(category);
		return ResponseEntity.status(201).body(new ApiResponse<>(saved, "Product category created", true,
				Instant.now()));
	}

	@Transactional
	public ResponseEntity<ApiResponse<ProductCategory>> renameCategory(String id, ProductCategoryRequest req,
			User actor) {
		ProductCategory category = categoryRepo.findById(id)
				.orElseThrow(() -> new EntityNotFoundException("Product category not found"));
		shopAccessService.requireOwnerShop(category.getShop().getId(), actor);
		if (!category.isActive()) {
			throw new IllegalArgumentException("Archived product categories cannot be renamed.");
		}

		String name = req.name().trim();
		categoryRepo.findByShop_IdAndNameIgnoreCase(category.getShop().getId(), name)
				.filter(existing -> !existing.getId().equals(id))
				.ifPresent(existing -> {
					throw new IllegalArgumentException(
							"A product category with this name already exists in this shop.");
				});
		category.setName(name);
		return ResponseEntity.ok(new ApiResponse<>(categoryRepo.save(category), "Product category updated", true,
				Instant.now()));
	}

	@Transactional
	public ResponseEntity<ApiResponse<String>> archiveCategory(String id, User actor) {
		ProductCategory category = categoryRepo.findById(id)
				.orElseThrow(() -> new EntityNotFoundException("Product category not found"));
		shopAccessService.requireOwnerShop(category.getShop().getId(), actor);
		category.setActive(false);
		categoryRepo.save(category);
		return ResponseEntity.ok(new ApiResponse<>("Product category archived", "Success", true, Instant.now()));
	}

	@Transactional
	public ResponseEntity<ApiResponse<ProductCategory>> restoreCategory(String id, User actor) {
		ProductCategory category = categoryRepo.findById(id)
				.orElseThrow(() -> new EntityNotFoundException("Product category not found"));
		shopAccessService.requireOwnerShop(category.getShop().getId(), actor);
		categoryRepo.findByShop_IdAndNameIgnoreCase(category.getShop().getId(), category.getName())
				.filter(existing -> existing.isActive() && !existing.getId().equals(id))
				.ifPresent(existing -> {
					throw new IllegalArgumentException(
							"An active product category with this name already exists in this shop.");
				});
		category.setActive(true);
		return ResponseEntity.ok(new ApiResponse<>(categoryRepo.save(category), "Product category restored", true,
				Instant.now()));
	}
}
