-- Before continuing, inspect retained debts whose customer was previously deleted.
-- They cannot be assigned a shop after the redundant shop_id is removed.
SELECT id, status, purge_at
FROM customer_debts
WHERE customer_id IS NULL;

-- Do not run the DDL below until the query above returns no rows.
SET @debt_shop_fk = (
	SELECT CONSTRAINT_NAME
	FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE
	WHERE TABLE_SCHEMA = DATABASE()
		AND TABLE_NAME = 'customer_debts'
		AND COLUMN_NAME = 'shop_id'
		AND REFERENCED_TABLE_NAME IS NOT NULL
	LIMIT 1
);

SET @drop_debt_shop_fk = IF(
	@debt_shop_fk IS NULL,
	'SELECT 1',
	CONCAT('ALTER TABLE `customer_debts` DROP FOREIGN KEY `', REPLACE(@debt_shop_fk, '`', '``'), '`')
);

PREPARE drop_debt_shop_fk_stmt FROM @drop_debt_shop_fk;
EXECUTE drop_debt_shop_fk_stmt;
DEALLOCATE PREPARE drop_debt_shop_fk_stmt;

ALTER TABLE customer_debts DROP COLUMN shop_id;
