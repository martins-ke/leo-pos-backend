package com.leo.pos.admin;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.leo.pos.sale.Sale;
import com.leo.pos.sale.SaleRepository;
import com.leo.pos.shop.Shop;
import com.leo.pos.shop.ShopRepository;
import com.leo.pos.staff.User;
import com.leo.pos.staff.UserRepository;

import jakarta.persistence.EntityNotFoundException;

@Service
public class SuperAdminService {

	private final UserRepository userRepo;
	private final ShopRepository shopRepo;
	private final SaleRepository saleRepo;

	public SuperAdminService(UserRepository userRepo, ShopRepository shopRepo, SaleRepository saleRepo) {
		this.userRepo = userRepo;
		this.shopRepo = shopRepo;
		this.saleRepo = saleRepo;
	}

	@Transactional(readOnly = true)
	public SuperAdminStatsDto getStats() {
		List<User> owners = userRepo.findAll().stream()
				.filter(u -> "OWNER".equalsIgnoreCase(u.getRole()))
				.toList();

		long activeOwners = owners.stream().filter(u -> "ACTIVE".equalsIgnoreCase(u.getStatus())).count();
		long totalOutlets = shopRepo.count();
		long totalCashiers = userRepo.findAll().stream().filter(u -> "STAFF".equalsIgnoreCase(u.getRole())).count();

		List<Sale> allSales = saleRepo.findAll();
		long totalTransactions = allSales.size();
		BigDecimal totalVolume = allSales.stream()
				.map(s -> s.getGrandTotal() != null ? s.getGrandTotal() : BigDecimal.ZERO)
				.reduce(BigDecimal.ZERO, BigDecimal::add);

		return new SuperAdminStatsDto(
				owners.size(),
				activeOwners,
				totalOutlets,
				totalCashiers,
				totalTransactions,
				totalVolume
		);
	}

	@Transactional(readOnly = true)
	public List<TenantSummaryDto> getAllTenants() {
		List<User> owners = userRepo.findAll().stream()
				.filter(u -> "OWNER".equalsIgnoreCase(u.getRole()))
				.toList();

		List<TenantSummaryDto> dtos = new ArrayList<>();
		for (User owner : owners) {
			List<Shop> shops = shopRepo.findByUserId(owner.getId());
			List<String> shopNames = shops.stream().map(Shop::getName).toList();

			// Count cashiers created by this owner
			long cashierCount = userRepo.findAll().stream()
					.filter(u -> owner.getId().equals(u.getCreatedBy()))
					.count();

			// Aggregate transactions across all shops owned by this tenant
			long transactions = 0;
			BigDecimal volume = BigDecimal.ZERO;
			for (Shop shop : shops) {
				List<Sale> sales = saleRepo.findByShopIdOrderByCreatedAtDesc(shop.getId());
				transactions += sales.size();
				for (Sale s : sales) {
					if (s.getGrandTotal() != null) {
						volume = volume.add(s.getGrandTotal());
					}
				}
			}

			dtos.add(new TenantSummaryDto(
					owner.getId(),
					owner.getUsername(),
					owner.getEmail(),
					owner.getStatus(),
					shops.size(),
					(int) cashierCount,
					transactions,
					volume,
					shopNames,
					shops.isEmpty() ? null : shops.get(0).getCreated_on()
			));
		}

		return dtos;
	}

	@Transactional
	public User toggleTenantStatus(String ownerId) {
		User owner = userRepo.findById(ownerId)
				.orElseThrow(() -> new EntityNotFoundException("Tenant not found"));

		String nextStatus = "ACTIVE".equalsIgnoreCase(owner.getStatus()) ? "INACTIVE" : "ACTIVE";
		owner.setStatus(nextStatus);
		return userRepo.save(owner);
	}
}
