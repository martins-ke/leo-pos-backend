package com.leo.pos.shop;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.leo.pos.ApiResponse;
import com.leo.pos.finance.ShopAccount;
import com.leo.pos.finance.ShopAccountRepository;
import com.leo.pos.staff.User;
import com.leo.pos.staff.UserRepository;

@Service
public class ShopService {
	private final ShopRepository shopRepo;
	private final UserRepository userRepo;
	private final ShopAccountRepository accountRepo;

	public ShopService(ShopRepository shopRepo, UserRepository userRepo, ShopAccountRepository accountRepo) {
		super();
		this.shopRepo = shopRepo;
		this.userRepo = userRepo;
		this.accountRepo = accountRepo;
	}

	@Transactional
	public ResponseEntity<ApiResponse<Shop>> register(ShopRequest req, User actor){
		String shopName = req.name();
		Shop shop = shopRepo.findByName(shopName).orElse(null);
		User user = userRepo.findById(actor.getId())
				.orElseThrow(() -> new IllegalArgumentException("Shop owner could not be found."));
		
		if(shop != null) return ResponseEntity.status(400).body(new ApiResponse<>(null, "Shop with name "+shopName+" exists! Please choose a unique name.", false, Instant.now())); 
		
		String shopId = UUID.randomUUID().toString();
		shop = new Shop(shopId, shopName, req.type(), req.location(), req.description(), LocalDate.now(), user);
		
		Shop savedShop = shopRepo.save(shop);
		accountRepo.save(new ShopAccount(UUID.randomUUID().toString(), savedShop));
		return ResponseEntity.status(201).body(new ApiResponse<>(savedShop, "Success, Shop created ✅", true, Instant.now())); 
	}
	
	public ResponseEntity<ApiResponse<List<Shop>>> getAllShops(User actor){
		List<Shop> shops;
		if ("OWNER".equalsIgnoreCase(actor.getRole())) {
			shops = shopRepo.findByUserId(actor.getId());
		} else {
			User freshActor = userRepo.findById(actor.getId()).orElse(actor);
			shops = freshActor.getShops() != null ? List.copyOf(freshActor.getShops()) : List.of();
		}
		List<Shop> newestFirst = shops.stream()
				.sorted(Comparator.comparing(Shop::getCreated_on,
						Comparator.nullsLast(Comparator.reverseOrder()))
						.thenComparing(Shop::getId, Comparator.nullsLast(Comparator.reverseOrder())))
				.toList();
		return ResponseEntity.status(200).body(new ApiResponse<>(newestFirst, "All Shops", true, Instant.now())); 
	}

	public ResponseEntity<ApiResponse<Shop>> getShopById(String id, User actor){
		Shop shop = shopRepo.findById(id).orElse(null);
		if (shop == null) {
			return ResponseEntity.status(404).body(new ApiResponse<>(null, "Shop not found", false, Instant.now()));
		}
		if (!"OWNER".equalsIgnoreCase(actor.getRole()) || shop.getOwnerId() == null
				|| !actor.getId().equals(shop.getOwnerId().getId())) {
			throw new org.springframework.security.access.AccessDeniedException("You do not own this shop");
		}
		return ResponseEntity.status(200).body(new ApiResponse<>(shop, "Shop details", true, Instant.now()));
	}

	public ResponseEntity<ApiResponse<String>> deleteShop(String id, User actor){
		Shop shop = shopRepo.findById(id).orElse(null);
		if (shop != null && (shop.getOwnerId() == null || !actor.getId().equals(shop.getOwnerId().getId()))) {
			throw new org.springframework.security.access.AccessDeniedException("You do not own this shop");
		}
		if (!shopRepo.existsById(id)) {
			return ResponseEntity.status(404).body(new ApiResponse<>(null, "Shop not found", false, Instant.now()));
		}
		shopRepo.deleteById(id);
		return ResponseEntity.status(200).body(new ApiResponse<>("Shop deleted successfully", "Success", true, Instant.now()));
	}
}
