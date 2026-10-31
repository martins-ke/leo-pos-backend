package com.leo.pos.finance;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.leo.pos.shop.Shop;
import com.leo.pos.shop.ShopRepository;
import com.leo.pos.staff.User;

import jakarta.persistence.EntityNotFoundException;

@Service
public class AccountLedgerService {

	private final ShopAccountRepository accountRepository;
	private final AccountTransactionRepository transactionRepository;
	private final ShopRepository shopRepository;

	public AccountLedgerService(ShopAccountRepository accountRepository,
			AccountTransactionRepository transactionRepository, ShopRepository shopRepository) {
		this.accountRepository = accountRepository;
		this.transactionRepository = transactionRepository;
		this.shopRepository = shopRepository;
	}

	@Transactional
	public ShopAccount initialize(Shop shop) {
		return getAccountForUpdate(shop);
	}

	@Transactional
	public AccountTransaction post(Shop shop, String type, String direction, BigDecimal amount,
			String paymentMethod, String description, String referenceType, String referenceId, User actor) {
		ShopAccount account = getAccountForUpdate(shop);
		return transactionRepository.save(new AccountTransaction(
				UUID.randomUUID().toString(), account, type, direction, amount,
				paymentMethod, description, referenceType, referenceId,
				actor.getId(), actor.getUsername()));
	}

	@Transactional
	public BigDecimal balanceForUpdate(Shop shop) {
		ShopAccount account = getAccountForUpdate(shop);
		return transactionRepository.calculateBalance(account.getId());
	}

	private ShopAccount getAccountForUpdate(Shop shop) {
		Shop lockedShop = shopRepository.findByIdForUpdate(shop.getId())
				.orElseThrow(() -> new EntityNotFoundException("Shop not found"));
		return accountRepository.findByShopIdForUpdate(shop.getId())
				.orElseGet(() -> accountRepository.saveAndFlush(
						new ShopAccount(UUID.randomUUID().toString(), lockedShop)));
	}
}
