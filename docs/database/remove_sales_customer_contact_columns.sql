-- Run against the leo_pos_db database after deploying the backend version
-- that no longer maps sales.customer_name and sales.customer_phone.
-- This drops the duplicated contact snapshots; linked customer records remain
-- available through sales.customer_id.

SET @drop_sales_customer_name = IF(
	EXISTS(
		SELECT 1
		FROM INFORMATION_SCHEMA.COLUMNS
		WHERE TABLE_SCHEMA = DATABASE()
			AND TABLE_NAME = 'sales'
			AND COLUMN_NAME = 'customer_name'
	),
	'ALTER TABLE `sales` DROP COLUMN `customer_name`',
	'SELECT 1'
);

PREPARE drop_sales_customer_name_stmt FROM @drop_sales_customer_name;
EXECUTE drop_sales_customer_name_stmt;
DEALLOCATE PREPARE drop_sales_customer_name_stmt;

SET @drop_sales_customer_phone = IF(
	EXISTS(
		SELECT 1
		FROM INFORMATION_SCHEMA.COLUMNS
		WHERE TABLE_SCHEMA = DATABASE()
			AND TABLE_NAME = 'sales'
			AND COLUMN_NAME = 'customer_phone'
	),
	'ALTER TABLE `sales` DROP COLUMN `customer_phone`',
	'SELECT 1'
);

PREPARE drop_sales_customer_phone_stmt FROM @drop_sales_customer_phone;
EXECUTE drop_sales_customer_phone_stmt;
DEALLOCATE PREPARE drop_sales_customer_phone_stmt;
