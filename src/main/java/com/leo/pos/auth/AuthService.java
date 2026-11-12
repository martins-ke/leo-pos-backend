package com.leo.pos.auth;

import java.time.Instant;
import java.time.LocalDate;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import com.leo.pos.ApiResponse;
import com.leo.pos.finance.ShopAccount;
import com.leo.pos.finance.ShopAccountRepository;
import com.leo.pos.security.JwtService;
import com.leo.pos.shop.Shop;
import com.leo.pos.shop.ShopRepository;
import com.leo.pos.staff.User;
import com.leo.pos.staff.UserRepository;
import com.leo.pos.util.IdGenarator;

@Service
public class AuthService {
	private final UserRepository userRepo; 
	private final PasswordEncoder passwordEncoder;
	private final JwtService jwtService;
	private final ShopRepository shopRepo;
	private final ShopAccountRepository accountRepo;
	private final String platformSetupKey;
	private final Environment environment;
	
	public AuthService(UserRepository userRepo, PasswordEncoder passwordEncoder,
			JwtService jwtService, ShopRepository shopRepo, ShopAccountRepository accountRepo,
			@Value("${leo.platform.setup-key:}") String platformSetupKey,
			Environment environment) {
		this.userRepo = userRepo;
		this.passwordEncoder = passwordEncoder;
		this.jwtService = jwtService;
		this.shopRepo = shopRepo;
		this.accountRepo = accountRepo;
		this.platformSetupKey = platformSetupKey;
		this.environment = environment;
	}

	public ResponseEntity<ApiResponse<PlatformSetupStatus>> getPlatformSetupStatus() {
		boolean setupKeyRequired = !platformSetupKey.isBlank();
		boolean platformOwnerExists = userRepo.findByRole("SUPER_ADMIN").isPresent();
		boolean setupAvailable = !platformOwnerExists
				&& (setupKeyRequired || isDevelopmentProfile());
		String message = setupAvailable
				? "Platform setup is available"
				: platformOwnerExists
						? "The Leo platform owner has already been set up."
						: "Platform setup requires LEO_PLATFORM_SETUP_KEY outside the development environment.";
		return ResponseEntity.ok(new ApiResponse<>(
				new PlatformSetupStatus(setupAvailable, setupKeyRequired),
				message,
				true,
				Instant.now()));
	}

	@Transactional(isolation = Isolation.SERIALIZABLE)
	public ResponseEntity<ApiResponse<AuthResponse>> setupPlatformOwner(
			PlatformSetupRequest request, String remoteAddress) {
		if (!platformSetupKey.isBlank()) {
			if (request.setupKey() == null || !MessageDigest.isEqual(
					platformSetupKey.getBytes(StandardCharsets.UTF_8),
					request.setupKey().getBytes(StandardCharsets.UTF_8))) {
				return ResponseEntity.status(403).body(new ApiResponse<>(
						null, "Invalid system setup key.", false, Instant.now()));
			}
		} else if (!isDevelopmentProfile() || !isLoopbackAddress(remoteAddress)) {
			return ResponseEntity.status(503).body(new ApiResponse<>(
					null, "Configure LEO_PLATFORM_SETUP_KEY to create the platform owner outside local development.", false, Instant.now()));
		}

		if (userRepo.findByRole("SUPER_ADMIN").isPresent()) {
			return ResponseEntity.status(409).body(new ApiResponse<>(
					null, "The Leo platform owner has already been set up.", false, Instant.now()));
		}

		String username = request.username().trim();
		String email = request.email() != null && !request.email().isBlank()
				? request.email().trim()
				: null;
		String password = request.password().trim();

		if (userRepo.findByUsername(username).isPresent()) {
			return ResponseEntity.badRequest().body(new ApiResponse<>(
					null, "Username already exists. Please choose another.", false, Instant.now()));
		}
		if (email != null && userRepo.findByEmail(email).isPresent()) {
			return ResponseEntity.badRequest().body(new ApiResponse<>(
					null, "Email address already exists. Please choose another.", false, Instant.now()));
		}
		if (!password.equals(request.confirm_password())) {
			return ResponseEntity.badRequest().body(new ApiResponse<>(
					null, "Passwords do not match.", false, Instant.now()));
		}

		String userId = IdGenarator.generateId(username, userRepo.count());
		User platformOwner = new User(
				userId,
				username,
				email,
				passwordEncoder.encode(password),
				"SUPER_ADMIN",
				"ACTIVE",
				"SYSTEM_SETUP");
		User savedUser = userRepo.save(platformOwner);
		String token = jwtService.generateToken(savedUser);

		return ResponseEntity.status(201).body(new ApiResponse<>(
				new AuthResponse(savedUser, token),
				"Leo platform owner account created.",
				true,
				Instant.now()));
	}

	private boolean isDevelopmentProfile() {
		return environment.acceptsProfiles(Profiles.of("dev"));
	}

	private boolean isLoopbackAddress(String remoteAddress) {
		if (remoteAddress == null || remoteAddress.isBlank()) {
			return false;
		}
		try {
			return InetAddress.getByName(remoteAddress).isLoopbackAddress();
		} catch (UnknownHostException e) {
			return false;
		}
	}

	@Transactional
	public ResponseEntity<ApiResponse<AuthResponse>> signUp(SignUpRequest request) {
		String password = request.password().trim(); 
		String username = request.username().trim();
		String email = request.email() != null && !request.email().isBlank() ? request.email().trim() : null;
		
		if (email != null && userRepo.findByEmail(email).isPresent()) {
			return ResponseEntity.status(400).body(new ApiResponse<>(null, "User with email " + email + " already exists! Email address must be unique.", false, Instant.now()));
		}
		if (!password.equals(request.confirm_password())) {
			return ResponseEntity.status(400).body(new ApiResponse<>(null, "Password mismatch! Please confirm and sign up again.", false, Instant.now())); 
		}
		
		String assignedRole = "OWNER";
		String initialStatus = "ACTIVE";
		
		String userId = IdGenarator.generateId(username, userRepo.count());
		String hashedPassword = passwordEncoder.encode(password); 
		
		User user = new User(userId, username, email, hashedPassword, assignedRole, initialStatus, "SELF");
		User savedUser = userRepo.save(user);

		// If onboarding as a new SaaS Shop Owner, automatically provision their initial default Shop and Account
		String shopId = null;
		if ("OWNER".equalsIgnoreCase(assignedRole)) {
			String rawBusinessName = request.businessName() != null && !request.businessName().isBlank()
					? request.businessName().trim()
					: username + " Retail Store";
			
			// Guarantee unique shop name across all tenants
			String uniqueShopName = rawBusinessName;
			if (shopRepo.findByName(uniqueShopName).isPresent()) {
				uniqueShopName = rawBusinessName + " (" + userId.substring(Math.max(0, userId.length() - 4)) + ")";
			}
			
			String shopType = request.businessType() != null && !request.businessType().isBlank() ? request.businessType() : "Retail Store";
			String location = request.businessLocation() != null && !request.businessLocation().isBlank() ? request.businessLocation() : "Main Branch";
			
			shopId = UUID.randomUUID().toString();
			Shop defaultShop = new Shop(shopId, uniqueShopName, shopType, location, "Primary outlet created on registration", LocalDate.now(), savedUser);
			Shop savedShop = shopRepo.save(defaultShop);
			
			// Open initial accounting ledger for the newly created shop
			accountRepo.save(new ShopAccount(UUID.randomUUID().toString(), savedShop));
		}
		
		String token = "ACTIVE".equalsIgnoreCase(savedUser.getStatus()) ? jwtService.generateToken(savedUser) : null;
		String successMsg = "Shop owner account created successfully.";
				
		return ResponseEntity.status(201).body(new ApiResponse<>(new AuthResponse(savedUser, token), successMsg, true, Instant.now())); 
	}
	
	public ResponseEntity<ApiResponse<AuthResponse>> login(LoginRequest request) {
		String identifier = request.username().trim();
		String password = request.password().trim();

		User user = userRepo.findByUsername(identifier)
				.orElseGet(() -> userRepo.findByEmail(identifier).orElse(null));

		if (user == null) {
			return ResponseEntity.status(400).body(new ApiResponse<>(null, "Invalid username/email or password.", false, Instant.now()));
		}

		boolean passwordMatches = passwordEncoder.matches(password, user.getPassword()); 
		if (!passwordMatches) {
			return ResponseEntity.status(400).body(new ApiResponse<>(null, "Invalid username/email or password.", false, Instant.now()));
		}

		if ("PENDING".equalsIgnoreCase(user.getStatus())) {
			return ResponseEntity.status(403).body(new ApiResponse<>(null, "Account pending owner approval. Please ask your shop owner to approve your account.", false, Instant.now()));
		}

		if ("INACTIVE".equalsIgnoreCase(user.getStatus()) || "DEACTIVATED".equalsIgnoreCase(user.getStatus())) {
			return ResponseEntity.status(403).body(new ApiResponse<>(null, "Your account has been deactivated. Please contact your manager.", false, Instant.now()));
		}

		if ("STAFF".equalsIgnoreCase(user.getRole()) && user.getCreatedBy() != null) {
			User shopOwner = userRepo.findById(user.getCreatedBy()).orElse(null);
			if (shopOwner != null
					&& "OWNER".equalsIgnoreCase(shopOwner.getRole())
					&& !"ACTIVE".equalsIgnoreCase(shopOwner.getStatus())) {
				return ResponseEntity.status(403).body(new ApiResponse<>(
						null,
						"Your shop is suspended. Please contact your shop owner before signing in.",
						false,
						Instant.now()));
			}
		}
		
		String token = jwtService.generateToken(user);
		return ResponseEntity.status(200).body(new ApiResponse<>(new AuthResponse(user, token), "Login successful", true, Instant.now()));
	}
}
