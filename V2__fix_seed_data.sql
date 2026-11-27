-- Fix seeded admin password (bcrypt of "Admin@123") and refresh coupon validity windows.
UPDATE users
   SET password = '$2a$10$eIOEseC6ii4DQ6F6lixPVeBHCsDBYidsWLGul8OfXLi5fmaY8RzuO'
 WHERE email = 'admin@foodapp.com';

UPDATE coupons
   SET valid_from  = '2024-01-01',
       valid_until = '2037-12-31 23:59:59'
 WHERE code IN ('WELCOME50','FLAT30','SAVE20');
