package com.leo.pos.finance;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ExpenseCategoryRepository extends JpaRepository<ExpenseCategory, String> {
	List<ExpenseCategory> findByShopIdOrderByNameAsc(String shopId);
	Optional<ExpenseCategory> findByShopIdAndNameIgnoreCase(String shopId, String name);
}
