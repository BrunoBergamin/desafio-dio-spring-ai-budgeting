-- Tipos escolhidos para funcionar igual no H2 (dev/testes) e no MySQL (perfil mysql):
--   id como VARCHAR(36): o Hibernate gravaria UUID nativo no H2 e BINARY(16) no MySQL
--   TIMESTAMP(6): o H2 2.x nao aceita DATETIME
CREATE TABLE transactions (
    id               VARCHAR(36)   NOT NULL,
    description      VARCHAR(120)  NOT NULL,
    amount           DECIMAL(12,2) NOT NULL,
    category         VARCHAR(20)   NOT NULL,
    transaction_date DATE          NOT NULL,
    created_at       TIMESTAMP(6)  NOT NULL,
    updated_at       TIMESTAMP(6),
    CONSTRAINT pk_transactions PRIMARY KEY (id)
);

CREATE INDEX idx_transactions_date ON transactions (transaction_date);
CREATE INDEX idx_transactions_category_date ON transactions (category, transaction_date);
