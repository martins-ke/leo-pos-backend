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
import org.springframework.security.crypto.password.PasswordEncoder;

import com.leo.pos.shop.Shop;
import com.leo.pos.shop.ShopRepository;
import com.leo.pos.customer.Customer;
import com.leo.pos.customer.CustomerRepository;
import com.leo.pos.finance.AccountTransaction;
import com.leo.pos.finance.AccountTransactionRepository;
import com.leo.pos.finance.ShopAccountRepository;
import com.leo.pos.product.Product;
import com.leo.pos.product.ProductRepository;
import com.leo.pos.product.ProductRequest;
import com.leo.pos.product.ProductService;
import com.leo.pos.sale.Sale;
import com.leo.pos.sale.SaleRepository;
import com.leo.pos.security.JwtService;
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
	private AccountTransactionRepository accountTransactionRepository;

	@Autowired
	private SaleRepository saleRepository;

	@Autowired
	private CustomerRepository customerRepository;

	@Autowired
	private ProductRepository productRepository;

	@Autowired
	private ProductService productService;

	@Autowired
	private JwtService jwtService;

	@Autowired
	private PasswordEncoder passwordEncoder;

	private String authorizationHeader(User user) {
		return "Bearer " + jwtService.generateToken(user);
	}

	@Test
	void contextLoads() {
	}

	@Test
	void healthEndpointIsAvailableWithoutAuthentication() throws Exception {
		mockMvc.perform(get("/actuator/health"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("UP"));
	}

	@Test
	void corsRejectsUnconfiguredOrigins() throws Exception {
		mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options("/dashboard/summary")
				.header("Origin", "https://untrusted.example")
				.header("Access-Control-Request-Method", "GET"))
				.andExpect(status().isForbidden());
	}

	@Test
	void jwtServiceRejectsShortSigningKeys() {
		org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
				() -> new JwtService("too-short", 60_000));
	}

	@Test
	void productPricesCannotBeSetBelowBuyingPrice() {
		ProductRequest request = new ProductRequest(
				"Below cost product", new BigDecimal("10.00"), new BigDecimal("9.99"), 1f, 0f, "pcs");

		IllegalArgumentException createException = org.junit.jupiter.api.Assertions.assertThrows(
				IllegalArgumentException.class,
				() -> productService.createProduct(request, "shop-id", null));
		org.junit.jupiter.api.Assertions.assertEquals(
				"Selling price must be greater than or equal to buying price.", createException.getMessage());

		IllegalArgumentException updateException = org.junit.jupiter.api.Assertions.assertThrows(
				IllegalArgumentException.class,
				() -> productService.updateProduct("product-id", request, null));
		org.junit.jupiter.api.Assertions.assertEquals(
				"Selling price must be greater than or equal to buying price.", updateException.getMessage());
	}

	@Test
	@Transactional
	void publicRegistrationAlwaysCreatesAnOwnerRegardlessOfSubmittedRole() throws Exception {
		mockMvc.perform(post("/auth/register")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "username": "public-owner-test",
						  "email": "public-owner-test@example.test",
						  "password": "secure-password",
						  "confirm_password": "secure-password",
						  "role": "STAFF",
						  "businessName": "Public Owner Test Shop"
						}
						"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.body.user.role").value("OWNER"));

		User owner = userRepository.findByUsername("public-owner-test").orElseThrow();
		org.junit.jupiter.api.Assertions.assertEquals("ACTIVE", owner.getStatus());
		org.junit.jupiter.api.Assertions.assertTrue(shopRepository.findByUserId(owner.getId()).stream()
				.anyMatch(shop -> "Public Owner Test Shop".equals(shop.getName())));

		mockMvc.perform(get("/admin/tenants")
				.header("Authorization", authorizationHeader(owner)))
				.andExpect(status().isForbidden());
	}

	@Test
	@Transactional
	void suspendedShopOwnerPreventsCashierLogin() throws Exception {
		User owner = userRepository.save(new User(
				"suspended-shop-owner", "suspended-shop-owner", "suspended-shop-owner@example.test",
				"unused", "OWNER", "INACTIVE", null));
		userRepository.save(new User(
				"suspended-shop-cashier", "suspended-shop-cashier", "suspended-shop-cashier@example.test",
				passwordEncoder.encode("cashier-password"), "STAFF", "ACTIVE", owner.getId()));

		mockMvc.perform(post("/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "username": "suspended-shop-cashier",
						  "password": "cashier-password"
						}
						"""))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.message").value("Your shop is suspended. Please contact your shop owner before signing in."));
	}

	@Test
	@Transactional
	void cashierCanLoginWhenTheirShopOwnerIsActive() throws Exception {
		User owner = userRepository.save(new User(
				"active-shop-owner", "active-shop-owner", "active-shop-owner@example.test",
				"unused", "OWNER", "ACTIVE", null));
		userRepository.save(new User(
				"active-shop-cashier", "active-shop-cashier", "active-shop-cashier@example.test",
				passwordEncoder.encode("cashier-password"), "STAFF", "ACTIVE", owner.getId()));

		mockMvc.perform(post("/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "username": "active-shop-cashier",
						  "password": "cashier-password"
						}
						"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.body.user.role").value("STAFF"));
	}

	@Test
	@Transactional
	void platformOwnerCanOnlyBeCreatedOnceWithConfiguredSetupKey() throws Exception {
		mockMvc.perform(get("/auth/setup/status"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.body.setupAvailable").value(true))
				.andExpect(jsonPath("$.body.setupKeyRequired").value(true));

		mockMvc.perform(post("/auth/setup")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "setupKey": "test-platform-setup-key",
						  "username": "platform-owner-test",
						  "email": "platform-owner-test@example.test",
						  "password": "secure-password",
						  "confirm_password": "secure-password"
						}
						"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.body.user.role").value("SUPER_ADMIN"))
				.andExpect(jsonPath("$.body.token").isNotEmpty());

		mockMvc.perform(post("/auth/setup")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "setupKey": "test-platform-setup-key",
						  "username": "second-platform-owner",
						  "password": "secure-password",
						  "confirm_password": "secure-password"
						}
						"""))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.success").value(false));
	}

	@Test
	void platformSetupRejectsAnInvalidSetupKey() throws Exception {
		mockMvc.perform(post("/auth/setup")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "setupKey": "incorrect-key",
						  "username": "unauthorized-platform-owner",
						  "password": "secure-password",
						  "confirm_password": "secure-password"
						}
						"""))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.success").value(false));

		org.junit.jupiter.api.Assertions.assertTrue(
				userRepository.findByUsername("unauthorized-platform-owner").isEmpty());
	}

	@Test
	@Transactional
	void userIdHeaderLoadsProtectedShopAndFinancialData() throws Exception {
		User owner = userRepository.save(new User(
				"test-owner", "test-owner", "test-owner@example.test", "unused", "OWNER", "ACTIVE", null));
		shopRepository.save(new Shop(
				"test-shop", "Test Shop", "GENERAL", "Test location", null, LocalDate.now(), owner));

		mockMvc.perform(get("/shops").header("Authorization", authorizationHeader(owner)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.body[0].id").value("test-shop"));

		mockMvc.perform(post("/accounts/test-shop/capital")
				.header("Authorization", authorizationHeader(owner))
				.header("Idempotency-Key", "test-capital-request")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"amount\":1250.00,\"paymentMethod\":\"CASH\",\"description\":\"Opening balance\"}"))
				.andExpect(status().isCreated());

		mockMvc.perform(get("/accounts/test-shop/summary").header("Authorization", authorizationHeader(owner)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.body.accountBalance").value(1250.0));

		mockMvc.perform(get("/accounts/test-shop/transactions").header("Authorization", authorizationHeader(owner)))
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
				.header("Authorization", authorizationHeader(owner))
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
	void staffProductAndSaleListsIncludeOnlyAssignedShops() throws Exception {
		User owner = userRepository.save(new User(
				"assigned-shop-owner", "assigned-shop-owner", "assigned-shop-owner@example.test",
				"unused", "OWNER", "ACTIVE", null));
		Shop assignedShop = shopRepository.save(new Shop(
				"assigned-shop", "Assigned Shop", "Retail", "Town", null, LocalDate.now(), owner));
		Shop unassignedShop = shopRepository.save(new Shop(
				"unassigned-shop", "Unassigned Shop", "Retail", "Town", null, LocalDate.now(), owner));
		User staff = new User(
				"assigned-shop-staff", "assigned-shop-staff", "assigned-shop-staff@example.test",
				"unused", "STAFF", "ACTIVE", owner.getId());
		staff.getShops().add(assignedShop);
		userRepository.save(staff);
		productRepository.save(new Product(
				"assigned-shop-product", "Assigned Product", BigDecimal.ONE, BigDecimal.TEN,
				5, 1, "item", true, assignedShop));
		productRepository.save(new Product(
				"unassigned-shop-product", "Unassigned Product", BigDecimal.ONE, BigDecimal.TEN,
				5, 1, "item", true, unassignedShop));
		saleRepository.save(new Sale("assigned-shop-sale", "receipt-assigned", BigDecimal.TEN,
				BigDecimal.TEN, BigDecimal.ZERO, "CASH", assignedShop, staff));
		saleRepository.save(new Sale("unassigned-shop-sale", "receipt-unassigned", BigDecimal.TEN,
				BigDecimal.TEN, BigDecimal.ZERO, "CASH", unassignedShop, owner));

		mockMvc.perform(get("/products")
				.header("Authorization", authorizationHeader(staff)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.body.length()").value(1))
				.andExpect(jsonPath("$.body[0].id").value("assigned-shop-product"));

		mockMvc.perform(get("/sales")
				.header("Authorization", authorizationHeader(staff)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.body.length()").value(1))
				.andExpect(jsonPath("$.body[0].id").value("assigned-shop-sale"));
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
				.header("Authorization", authorizationHeader(owner)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.body[0].id").value("sale-newest"))
				.andExpect(jsonPath("$.body[1].id").value("sale-oldest"));
	}

	@Test
	@Transactional
	void convertsPaidSaleToLinkedCustomerDebtAndReversesCollectedPayment() throws Exception {
		User owner = userRepository.save(new User(
				"sale-debt-owner", "sale-debt-owner", "sale-debt-owner@example.test",
				"unused", "OWNER", "ACTIVE", null));
		Shop shop = shopRepository.save(new Shop(
				"sale-debt-shop", "Sale Debt Shop", "Retail", "Town", null, LocalDate.now(), owner));
		Customer customer = new Customer(
				"sale-debt-customer", "Credit Customer", null, null, shop, owner.getId());
		customer.setCreditEligible(true);
		customerRepository.save(customer);
		Sale sale = new Sale("sale-to-convert", "receipt-to-convert", new BigDecimal("125.00"),
				new BigDecimal("125.00"), BigDecimal.ZERO, "CASH", shop, owner);
		sale.setCustomer(customer);
		saleRepository.save(sale);

		mockMvc.perform(post("/debts/from-sale/{saleId}", sale.getId())
				.header("Authorization", authorizationHeader(owner))
				.header("Idempotency-Key", "convert-sale-to-debt-test"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.body.customerId").value(customer.getId()))
				.andExpect(jsonPath("$.body.principal").value(125.0))
				.andExpect(jsonPath("$.body.saleId").value(sale.getId()));

		Sale convertedSale = saleRepository.findById(sale.getId()).orElseThrow();
		org.junit.jupiter.api.Assertions.assertEquals("DEBT", convertedSale.getPaymentMethod());
		org.junit.jupiter.api.Assertions.assertEquals(0, BigDecimal.ZERO.compareTo(convertedSale.getAmountTendered()));
		java.util.List<AccountTransaction> reversals = accountTransactionRepository
				.findByAccountShopIdAndTypeAndOccurredAtAfter(
						shop.getId(), "SALE_CREDIT_CONVERSION", java.time.Instant.EPOCH);
		org.junit.jupiter.api.Assertions.assertEquals(1, reversals.size());
		org.junit.jupiter.api.Assertions.assertEquals("OUT", reversals.get(0).getDirection());

		mockMvc.perform(post("/debts/from-sale/{saleId}", sale.getId())
				.header("Authorization", authorizationHeader(owner))
				.header("Idempotency-Key", "convert-sale-to-debt-again-test"))
				.andExpect(status().isBadRequest());
	}

	@Test
	@Transactional
	void posDebtSaleCreatesLinkedDebtWithoutRecordingCashReceipt() throws Exception {
		User owner = userRepository.save(new User(
				"pos-debt-owner", "pos-debt-owner", "pos-debt-owner@example.test",
				"unused", "OWNER", "ACTIVE", null));
		Shop shop = shopRepository.save(new Shop(
				"pos-debt-shop", "POS Debt Shop", "Retail", "Town", null, LocalDate.now(), owner));
		Customer customer = new Customer(
				"pos-debt-customer", "POS Credit Customer", null, null, shop, owner.getId());
		customer.setCreditEligible(true);
		customerRepository.save(customer);
		productRepository.save(new Product(
				"pos-debt-product", "Debt Product", new BigDecimal("5.00"), new BigDecimal("12.50"),
				5, 1, "item", true, shop));

		mockMvc.perform(post("/sales")
				.header("Authorization", authorizationHeader(owner))
				.header("Idempotency-Key", "pos-debt-sale-test")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "shopId": "pos-debt-shop",
						  "items": [{"productId": "pos-debt-product", "quantity": 2, "unitPrice": 12.50}],
						  "grandTotal": 25.00,
						  "amountTendered": 0,
						  "changeGiven": 0,
						  "paymentMethod": "DEBT",
						  "customerId": "pos-debt-customer"
						}
						"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.body.paymentMethod").value("DEBT"))
				.andExpect(jsonPath("$.body.amountTendered").value(0))
				.andExpect(jsonPath("$.body.customer.creditEligible").value(true));

		mockMvc.perform(get("/debts")
				.header("Authorization", authorizationHeader(owner))
				.param("shopId", shop.getId()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.body.content[0].principal").value(25.0))
				.andExpect(jsonPath("$.body.content[0].saleId").isNotEmpty());
		org.junit.jupiter.api.Assertions.assertTrue(accountTransactionRepository
				.findByAccountShopIdAndTypeAndOccurredAtAfter(
						shop.getId(), "SALE_RECEIPT", java.time.Instant.EPOCH)
				.isEmpty());
	}

}
