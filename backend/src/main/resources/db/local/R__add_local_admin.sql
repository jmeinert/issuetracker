INSERT INTO users (
    id,
    email,
    display_name,
    password_hash,
    role,
    enabled,
    created_at
)
VALUES (
   nextval('user_id_seq'),
   'admin@localhost.invalid',
   'The Admin',
   '{argon2id}$argon2id$v=19$m=19456,t=2,p=1$FqSGFzIjoqD2RjA9GILPIA$d+cQ8t4R2ZpT7ftcvKV2ZcLfAcrV7LcQ6PGw1GRg0k8',
   'ADMIN',
   TRUE,
   now()
)
ON CONFLICT DO NOTHING;
