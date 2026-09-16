-- Telefone (so digitos, com DDI, ex.: 5519999999999) para vincular a conta ao WhatsApp
ALTER TABLE users ADD COLUMN phone VARCHAR(20);
ALTER TABLE users ADD CONSTRAINT uk_users_phone UNIQUE (phone);
