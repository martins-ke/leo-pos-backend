package com.leo.pos;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.math.BigDecimal;

import org.springframework.http.MediaType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.leo.pos.shop.Shop;
import com.leo.pos.shop.ShopRepository;
import com.leo.pos.finance.ShopAccountRepository;
import com.leo.pos.sale.Sale;
import com.leo.pos.sale.SaleRepository;
import com.leo.pos.staff.User;
import com.leo.pos.staff.UserRepository;

@AutoConfigureMockMvc
@SpringBootTest
class LeoApplicationTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private ShopRepository shopRepository;

	@Autowired
	private ShopAccountRepository shopAccountRepository;

	@Autowired
	private SaleRepository saleRepository;

	@Test
	void contextLoads() {
	}

	@Test
	@Transactional
	void userIdHeaderLoadsProtectedShopAndFinancialData() throws Exception {
		User owner = userRepository.save(new User(
				"test-owner", "test-owner", "test-owner@example.test", "unused", "OWNER", "ACTIVE", null));
		shopRepository.save(new Shop(
				"test-shop", "Test Shop", "GENERAL", "Test location", null, LocalDate.now(), owner));

		mockMvc.perform(get("/shops").header("X-User-Id", "test-owner"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.body[0].id").value("test-shop"));

		mockMvc.perform(post("/accounts/test-shop/capital")
				.header("X-User-Id", "test-owner")
				.header("Idempotency-Key", "test-capital-request")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"amount\":1250.00,\"paymentMethod\":\"CASH\",\"description\":\"Opening balance\"}"))
				.andExpect(status().isCreated());

		mockMvc.perform(get("/accounts/test-shop/summary").header("X-User-Id", "test-owner"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.body.accountBalance").value(1250.0));

		mockMvc.perform(get("/accounts/test-shop/transactions").header("X-User-Id", "test-owner"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.body.content[0].amount").value(1250.0));
	}

	@Test
	void createsShopWhenCountBasedIdentifierWouldCollide() throws Exception {
		User owner = userRepository.save(new User(
				"shop-create-owner", "shop-create-owner", "shop-create-owner@example.test",
				"unused", "OWNER", "ACTIVE", null));
		shopRepository.save(new Shop(
				"SH-0002", "Shop Previously Created", "Retail", "Town", null, LocalDate.now(), owner));

		mockMvc.perform(post("/shops")
				.header("X-User-Id", owner.getId())
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\":\"Shop New Outlet\",\"type\":\"Retail\",\"location\":\"Town\",\"description\":\"\"}"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.body.name").value("Shop New Outlet"));

		Shop createdShop = shopRepository.findByName("Shop New Outlet").orElseThrow();
		org.junit.jupiter.api.Assertions.assertTrue(
				shopAccountRepository.findByShopId(createdShop.getId()).isPresent());
	}

	@Test
	@Transactional
	void salesAcrossShopsAreReturnedNewestFirst() throws Exception {
		User owner = userRepository.save(new User(
				"sales-order-owner", "sales-order-owner", "sales-order-owner@example.test",
				"unused", "OWNER", "ACTIVE", null));
		Shop firstShop = shopRepository.save(new Shop(
				"sales-order-shop-a", "Sales Order Shop A", "Retail", "Town", null, LocalDate.now(), owner));
		Shop secondShop = shopRepository.save(new Shop(
				"sales-order-shop-b", "Sales Order Shop B", "Retail", "Town", null, LocalDate.now(), owner));
		saleRepository.save(new Sale("sale-oldest", "receipt-oldest", BigDecimal.TEN,
				BigDecimal.TEN, BigDecimal.ZERO, "CASH", firstShop, owner));
		saleRepository.save(new Sale("sale-newest", "receipt-newest", BigDecimal.TEN,
				BigDecimal.TEN, BigDecimal.ZERO, "CASH", secondShop, owner));
		saleRepository.findById("sale-oldest").orElseThrow()
				.setCreatedAt(LocalDateTime.parse("2026-01-01T10:00:00"));
		saleRepository.findById("sale-newest").orElseThrow()
				.setCreatedAt(LocalDateTime.parse("2026-01-02T10:00:00"));
		saleRepository.flush();

		mockMvc.perform(get("/sales")
				.header("X-User-Id", owner.getId()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.body[0].id").value("sale-newest"))
				.andExpect(jsonPath("$.body[1].id").value("sale-oldest"));
	}

}
