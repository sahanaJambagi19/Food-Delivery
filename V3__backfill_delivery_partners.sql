-- Ensure every DELIVERY_PARTNER user has a delivery_partners row.
INSERT INTO delivery_partners (user_id, is_available, total_deliveries, total_earnings)
SELECT u.id, FALSE, 0, 0
  FROM users u
  JOIN roles r ON r.id = u.role_id
 WHERE r.name = 'DELIVERY_PARTNER'
   AND NOT EXISTS (SELECT 1 FROM delivery_partners d WHERE d.user_id = u.id);
