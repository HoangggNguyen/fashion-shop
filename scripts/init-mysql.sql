CREATE DATABASE IF NOT EXISTS auth_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS catalog_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS inventory_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS cart_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS promotion_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS order_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS payment_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS notification_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE USER IF NOT EXISTS 'shop_user'@'localhost' IDENTIFIED BY 'shop_password';
CREATE USER IF NOT EXISTS 'shop_user'@'%' IDENTIFIED BY 'shop_password';

GRANT ALL PRIVILEGES ON auth_db.* TO 'shop_user'@'localhost';
GRANT ALL PRIVILEGES ON catalog_db.* TO 'shop_user'@'localhost';
GRANT ALL PRIVILEGES ON inventory_db.* TO 'shop_user'@'localhost';
GRANT ALL PRIVILEGES ON cart_db.* TO 'shop_user'@'localhost';
GRANT ALL PRIVILEGES ON promotion_db.* TO 'shop_user'@'localhost';
GRANT ALL PRIVILEGES ON order_db.* TO 'shop_user'@'localhost';
GRANT ALL PRIVILEGES ON payment_db.* TO 'shop_user'@'localhost';
GRANT ALL PRIVILEGES ON notification_db.* TO 'shop_user'@'localhost';

GRANT ALL PRIVILEGES ON auth_db.* TO 'shop_user'@'%';
GRANT ALL PRIVILEGES ON catalog_db.* TO 'shop_user'@'%';
GRANT ALL PRIVILEGES ON inventory_db.* TO 'shop_user'@'%';
GRANT ALL PRIVILEGES ON cart_db.* TO 'shop_user'@'%';
GRANT ALL PRIVILEGES ON promotion_db.* TO 'shop_user'@'%';
GRANT ALL PRIVILEGES ON order_db.* TO 'shop_user'@'%';
GRANT ALL PRIVILEGES ON payment_db.* TO 'shop_user'@'%';
GRANT ALL PRIVILEGES ON notification_db.* TO 'shop_user'@'%';

FLUSH PRIVILEGES;
