package com.leo.pos.productCategories;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.leo.pos.product.Product;
import com.leo.pos.product.ProductRepository;
import com.leo.pos.product.ProductRequest;
import com.leo.pos.product.ProductService;
import com.leo.pos.security.JwtService;
import com.leo.pos.shop.Shop;
import com.leo.pos.shop.ShopRepository;
import com.leo.pos.staff.User;
import com.leo.pos.staff.UserRepository;

@AutoConfigureMockMvc
@SpringBootTest
class ProductCategoryIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ProductCategoryService categoryService;

	@Autowired
	private ProductService productService;

	@Autowired
	private ProductRepository productRepository;

	@Autowired
	private ShopRepository shopRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private JwtService jwtService;

	@Test
	@Transactional
	void categoryAndProductPageEndpointsAreMappedAndAllowCategoryCreation() throws Exception {
		String suffix = UUID.randomUUID().toString();
		User owner = userRepository.save(new User("route-owner-" + suffix, "route-owner-" + suffix,
				"route-owner-" + suffix + "@example.test", "unused", "OWNER", "ACTIVE", null));
		Shop shop = shopRepository.save(new Shop("route-shop-" + suffix, "Route Shop " + suffix,
				"Retail", "Town", null, LocalDate.now(), owner));
		String authorization = "Bearer " + jwtService.generateToken(owner);

		mockMvc.perform(get("/products")
				.param("shopId", shop.getId())
				.header("Authorization", authorization))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true));

		mockMvc.perform(get("/product-categories")
				.param("shopId", shop.getId())
				.header("Authorization", authorization))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.body").isArray());

		mockMvc.perform(get("/product-categories/suggestions")
				.header("Authorization", authorization))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.body").isArray());

		mockMvc.perform(post("/product-categories/" + shop.getId())
				.header("Authorization", authorization)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\":\"Beverages\"}"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.body.name").value("Beverages"));
	}

	@Test
	@Transactional
	void categoryAssignmentIsShopScopedAndArchivingPreservesProductHistory() {
		String suffix = UUID.randomUUID().toString();
		User owner = userRepository.save(new User("owner-" + suffix, "owner-" + suffix,
				"owner-" + suffix + "@example.test", "unused", "OWNER", "ACTIVE", null));
		Shop shop = shopRepository.save(new Shop("shop-" + suffix, "Shop " + suffix, "Retail",
				"Town", null, LocalDate.now(), owner));
		Shop otherShop = shopRepository.save(new Shop("other-shop-" + suffix, "Other Shop " + suffix,
				"Retail", "Town", null, LocalDate.now(), owner));

		ProductCategory category = categoryService.createCategory(
				new ProductCategoryRequest("  Beverages  "), shop.getId(), owner).getBody().body();
		assertNotNull(category);
		assertEquals("Beverages", category.getName());
		assertTrue(category.isActive());
		assertThrows(IllegalArgumentException.class, () -> categoryService.createCategory(
				new ProductCategoryRequest("beverages"), shop.getId(), owner));

		Product product = productService.createProduct(new ProductRequest("Water",
				new BigDecimal("10.00"), new BigDecimal("15.00"), 12f, 2f, "pcs", category.getId()),
				shop.getId(), owner).getBody().body();
		assertNotNull(product);
		assertEquals(category.getId(), product.getCategory().getId());

		ProductCategory otherCategory = categoryService.createCategory(
				new ProductCategoryRequest("Beverages"), otherShop.getId(), owner).getBody().body();
		assertThrows(IllegalArgumentException.class, () -> productService.createProduct(
				new ProductRequest("Other water", BigDecimal.ONE, BigDecimal.TEN, 1f, 0f, "pcs",
						otherCategory.getId()),
				shop.getId(), owner));

		productService.updateProduct(product.getId(), new ProductRequest("Water",
				new BigDecimal("10.00"), new BigDecimal("15.00"), 12f, 2f, "pcs"), owner);
		assertEquals(category.getId(), productRepository.findById(product.getId()).orElseThrow()
				.getCategory().getId());

		categoryService.archiveCategory(category.getId(), owner);
		assertTrue(categoryService.getActiveCategories(shop.getId(), owner).getBody().body().isEmpty());
		Product retainedProduct = productRepository.findById(product.getId()).orElseThrow();
		assertEquals(category.getId(), retainedProduct.getCategory().getId());
		assertFalse(retainedProduct.getCategory().isActive());

		productService.updateProduct(product.getId(), new ProductRequest("Water",
				new BigDecimal("10.00"), new BigDecimal("15.00"), 12f, 2f, "pcs", null, true), owner);
		assertNull(productRepository.findById(product.getId()).orElseThrow().getCategory());

		categoryService.restoreCategory(category.getId(), owner);
		assertEquals(1, categoryService.getActiveCategories(shop.getId(), owner).getBody().body().size());
	}
}
