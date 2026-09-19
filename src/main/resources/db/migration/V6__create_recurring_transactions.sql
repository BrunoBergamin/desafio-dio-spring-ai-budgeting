-- Contas que se repetem todo mes (aluguel, streaming, salario). A regra fica aqui; o lancamento de cada
-- mes continua na tabela transactions, apontando de volta por recurring_id.
CREATE TABLE recurring_transactions (
    id                   VARCHAR(36)   NOT NULL,
    user_id              VARCHAR(36)   NOT NULL,
    description          VARCHAR(120)  NOT NULL,
    amount               DECIMAL(12,2) NOT NULL,
    category             VARCHAR(20)   NOT NULL,
    type                 VARCHAR(10)   NOT NULL,
    day_of_month         INT           NOT NULL,
    active               BOOLEAN       NOT NULL DEFAULT TRUE,
    start_month          DATE          NOT NULL,
    end_month            DATE,
    -- Ultimo mes ja gerado: e o que impede criar o mesmo lancamento duas vezes
    last_generated_month DATE,
    created_at           TIMESTAMP(6)  NOT NULL,
    updated_at           TIMESTAMP(6),
    CONSTRAINT pk_recurring_transactions PRIMARY KEY (id),
    CONSTRAINT fk_recurring_user FOREIGN KEY (user_id) REFERENCES users (id)
);

-- O gerador diario varre so as regras ativas
CREATE INDEX idx_recurring_user_active ON recurring_transactions (user_id, active);

-- Apagar a regra nao apaga o historico: o lancamento fica, so perde o vinculo
ALTER TABLE transactions ADD COLUMN recurring_id VARCHAR(36);
ALTER TABLE transactions ADD CONSTRAINT fk_transactions_recurring
    FOREIGN KEY (recurring_id) REFERENCES recurring_transactions (id) ON DELETE SET NULL;
CREATE INDEX idx_transactions_recurring ON transactions (recurring_id);
