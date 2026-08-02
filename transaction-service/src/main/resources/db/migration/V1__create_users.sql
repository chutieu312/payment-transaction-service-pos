CREATE TABLE IF NOT EXISTS users (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email       VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role        VARCHAR(50)  NOT NULL DEFAULT 'CUSTOMER',
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- Seed admin and demo users (passwords: admin123 / customer123)
INSERT INTO users (email, password_hash, role) VALUES
  ('admin@bank.com',    '$2b$10$j6NhqxHWYlRmz.4LDvVExeElNnOr.uc9fwY2MExct5rifGAPNzrWS', 'BANK_ADMIN'),
  ('alice@example.com', '$2b$10$d3FCdNrbGNv.xRGgMw3/h.DW8wR8W5y8KeQM/4EsP2phYOhW9V3IS', 'CUSTOMER'),
  ('bob@example.com',   '$2b$10$d3FCdNrbGNv.xRGgMw3/h.DW8wR8W5y8KeQM/4EsP2phYOhW9V3IS', 'CUSTOMER')
ON CONFLICT (email) DO NOTHING;
