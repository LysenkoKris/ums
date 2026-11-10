INSERT INTO roles (code, name)
SELECT 'GUEST', 'Гость'
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE code = 'GUEST');
