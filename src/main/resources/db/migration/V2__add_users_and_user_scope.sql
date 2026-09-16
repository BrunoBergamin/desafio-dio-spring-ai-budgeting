-- "users" e nao "user": USER e palavra reservada no H2
CREATE TABLE users (
    id            VARCHAR(36)  NOT NULL,
    name          VARCHAR(80)  NOT NULL,
    email         VARCHAR(180) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    created_at    TIMESTAMP(6) NOT NULL,
    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT uk_users_email UNIQUE (email)
);

-- Projeto de demonstracao: o H2 e em memoria e o MySQL so tem dados de teste.
-- Esvaziar a tabela permite criar a coluna ja como NOT NULL de forma portatil
-- (tornar uma coluna existente NOT NULL tem sintaxe diferente em cada banco).
DELETE FROM transactions;

ALTER TABLE transactions ADD COLUMN user_id VARCHAR(36) NOT NULL;
ALTER TABLE transactions ADD CONSTRAINT fk_transactions_user FOREIGN KEY (user_id) REFERENCES users (id);
CREATE INDEX idx_transactions_user_date ON transactions (user_id, transaction_date);
