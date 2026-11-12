package com.leo.pos.productCategories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductCategoryRepository extends JpaRepository<ProductCategory, String> {

	List<ProductCategory> findByShop_IdAndActiveTrueOrderByNameAsc(String shopId);

	Optional<ProductCategory> findByShop_IdAndNameIgnoreCase(String shopId, String name);

	Optional<ProductCategory> findByIdAndShop_Id(String id, String shopId);
}
