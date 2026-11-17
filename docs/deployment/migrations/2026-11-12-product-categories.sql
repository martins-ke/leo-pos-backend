-- Apply once to a backed-up production database before deploying category-aware code.
-- Resolve any rows returned by the preflight queries before applying the DDL.

SELECT id, shop_id, name
FROM product_categories
WHERE shop_id IS NULL;

SELECT shop_id, LOWER(name) AS normalized_name, COUNT(*) AS duplicate_count
FROM product_categories
GROUP BY shop_id, LOWER(name)
HAVING COUNT(*) > 1;

SELECT pc.id, pc.shop_id
FROM product_categories pc
LEFT JOIN shops s ON s.id = pc.shop_id
WHERE s.id IS NULL;

ALTER TABLE product_categories
	ADD COLUMN active BOOLEAN NOT NULL DEFAULT TRUE,
	MODIFY COLUMN shop_id VARCHAR(255) NOT NULL,
	ADD CONSTRAINT uk_product_category_shop_name UNIQUE (shop_id, name),
	ADD CONSTRAINT fk_product_categories_shop
		FOREIGN KEY (shop_id) REFERENCES shops (id) ON DELETE RESTRICT;

ALTER TABLE products
	ADD COLUMN category_id VARCHAR(255) NULL,
	ADD INDEX idx_products_category_id (category_id),
	ADD CONSTRAINT fk_products_category
		FOREIGN KEY (category_id) REFERENCES product_categories (id) ON DELETE RESTRICT;
