package com.leo.pos.security;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import com.leo.pos.shop.Shop;
import com.leo.pos.shop.ShopRepository;
import com.leo.pos.staff.User;

import jakarta.persistence.EntityNotFoundException;

@Service
public class ShopAccessService {

	private final ShopRepository shopRepository;

	public ShopAccessService(ShopRepository shopRepository) {
		this.shopRepository = shopRepository;
	}

	public Shop requireShopAccess(String shopId, User actor) {
		Shop shop = shopRepository.findById(shopId)
				.orElseThrow(() -> new EntityNotFoundException("Shop not found"));
		if (actor == null || !"ACTIVE".equalsIgnoreCase(actor.getStatus())) {
			throw new AccessDeniedException("An active user is required");
		}
		if ("OWNER".equalsIgnoreCase(actor.getRole())) {
			if (shop.getOwnerId() == null || !actor.getId().equals(shop.getOwnerId().getId())) {
				throw new AccessDeniedException("You do not have access to this shop");
			}
			return shop;
		}
		// Staff users must be explicitly assigned to the shop they are accessing
		if ("STAFF".equalsIgnoreCase(actor.getRole())) {
			// Ensure the staff belongs to the owner of the shop
			if (actor.getCreatedBy() == null || shop.getOwnerId() == null || !actor.getCreatedBy().equals(shop.getOwnerId().getId())) {
				throw new AccessDeniedException("You do not have access to this shop");
			}
			// Verify the staff is assigned to this shop
			if (actor.getShops() == null || actor.getShops().stream().noneMatch(s -> s.getId().equals(shop.getId()))) {
				throw new AccessDeniedException("Staff not assigned to this shop");
			}
			return shop;
		}
		// If not staff (should be owner, already handled), deny access
		throw new AccessDeniedException("You do not have access to this shop");
	}

	public Shop requireOwnerShop(String shopId, User actor) {
		Shop shop = requireShopAccess(shopId, actor);
		if (!"OWNER".equalsIgnoreCase(actor.getRole())) {
			throw new AccessDeniedException("Only the shop owner can perform this operation");
		}
		return shop;
	}
}
